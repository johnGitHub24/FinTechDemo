package com.fintech.demo.account.application;

import com.fintech.demo.common.dto.AccountDto;
import com.fintech.demo.common.dto.ApplyTradeRequest;
import com.fintech.demo.common.dto.PositionDto;
import com.fintech.demo.common.event.TradeExecutedEvent;
import com.fintech.demo.account.common.BusinessException;
import com.fintech.demo.account.common.NotFoundException;
import com.fintech.demo.account.infrastructure.AccountEntity;
import com.fintech.demo.account.infrastructure.AccountRepository;
import com.fintech.demo.account.infrastructure.PositionEntity;
import com.fintech.demo.account.infrastructure.PositionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 【職責】帳戶／持倉帳本：查詢、成交入帳、Demo seed。
 * 【技巧】BUY 扣現金＋加權均價；SELL 減持倉＋加現金；notional 缺省時用 price×qty。
 * 【概念】帳本是單一寫入點；Redis 只是讀側快取，寫入後由 QueryService 清 key。
 */
@Service
public class AccountLedgerService {

    private final AccountRepository accountRepository;
    private final PositionRepository positionRepository;

    public AccountLedgerService(AccountRepository accountRepository, PositionRepository positionRepository) {
        this.accountRepository = accountRepository;
        this.positionRepository = positionRepository;
    }

    /**
     * 【職責】讀取指定使用者的帳戶資料並轉為跨服務 DTO。
     * 【技巧】使用 readOnly 交易與 repository 的 userId 查詢，缺失時明確拋出 404 業務例外。
     * 【概念】帳本讀取以 userId 作資料隔離的核心索引。
     */
    @Transactional(readOnly = true)
    public AccountDto getAccount(Long userId) {
        AccountEntity a = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("account not found"));
        return toAccountDto(a);
    }

    /**
     * 【職責】讀取指定使用者全部持倉並轉換為 DTO。
     * 【技巧】在 readOnly 交易中以 stream 統一映射 entity。
     * 【概念】持倉是帳本的衍生讀模型，不能由外部直接修改。
     */
    @Transactional(readOnly = true)
    public List<PositionDto> listPositions(Long userId) {
        return positionRepository.findByUserId(userId).stream()
                .map(this::toPositionDto)
                .toList();
    }

    /**
     * 【職責】將非同步成交事件轉接為帳本入帳請求。
     * 【技巧】驗證事件後複製必要欄位到 ApplyTradeRequest，再共用同步入帳邏輯。
     * 【概念】事件與 HTTP 兩種輸入共用同一帳本規則可避免行為漂移。
     */
    @Transactional
    public AccountDto applyTrade(TradeExecutedEvent event) {
        if (event == null || event.userId() == null) {
            throw new BusinessException("invalid trade event");
        }
        ApplyTradeRequest req = new ApplyTradeRequest();
        req.setOrderId(event.orderId());
        req.setUserId(event.userId());
        req.setSymbol(event.symbol());
        req.setSide(event.side());
        req.setQuantity(event.quantity());
        req.setPrice(event.price());
        req.setNotional(event.notional());
        return applyTrade(req);
    }

    /**
     * 【職責】依買賣方向更新現金與持倉並回傳最新帳戶。
     * 【技巧】先驗證交易、計算名義金額，再在單一交易中完成帳戶與持倉更新。
     * 【概念】帳本更新應是唯一寫入點，確保現金與持倉的一致性。
     */
    @Transactional
    public AccountDto applyTrade(ApplyTradeRequest req) {
        validateTrade(req);
        Long userId = req.getUserId();
        AccountEntity account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("account not found"));
        BigDecimal notional = resolveNotional(req);
        String side = req.getSide().trim().toUpperCase();

        if ("BUY".equals(side)) {
            if (account.getCashBalance().compareTo(notional) < 0) {
                throw new BusinessException("insufficient cash");
            }
            account.setCashBalance(account.getCashBalance().subtract(notional));
            accountRepository.save(account);
            upsertBuyPosition(userId, req.getSymbol(), req.getQuantity(), req.getPrice());
        } else if ("SELL".equals(side)) {
            PositionEntity pos = positionRepository.findByUserIdAndSymbol(userId, req.getSymbol())
                    .orElseThrow(() -> new BusinessException("no position to sell"));
            if (pos.getQuantity() < req.getQuantity()) {
                throw new BusinessException("insufficient position");
            }
            pos.setQuantity(pos.getQuantity() - req.getQuantity());
            positionRepository.save(pos);
            account.setCashBalance(account.getCashBalance().add(notional));
            accountRepository.save(account);
        } else {
            throw new BusinessException("side must be BUY or SELL");
        }
        return toAccountDto(account);
    }

    /**
     * Demo 種子：若帳戶已存在則跳過（idempotent）。
     */
    @Transactional
    public void seedAccount(Long userId, BigDecimal cash, String currency) {
        if (accountRepository.findByUserId(userId).isPresent()) {
            return;
        }
        AccountEntity a = new AccountEntity();
        a.setUserId(userId);
        a.setCashBalance(cash);
        a.setCurrency(currency);
        accountRepository.save(a);
    }

    /**
     * 【職責】冪等建立展示持倉。
     * 【技巧】用 userId 與 symbol 作存在性判斷，僅在尚未建立時保存。
     * 【概念】持倉的自然鍵是使用者與商品的組合。
     */
    @Transactional
    public void seedPosition(Long userId, String symbol, int quantity, BigDecimal avgPrice) {
        if (positionRepository.findByUserIdAndSymbol(userId, symbol).isPresent()) {
            return;
        }
        PositionEntity p = new PositionEntity();
        p.setUserId(userId);
        p.setSymbol(symbol);
        p.setQuantity(quantity);
        p.setAvgPrice(avgPrice);
        positionRepository.save(p);
    }

    /**
     * 【職責】買入時建立或更新持倉數量及加權平均成本。
     * 【技巧】既有持倉以舊成本加新成本除以新數量，採四位 HALF_UP。
     * 【概念】平均成本是長期持倉估值與後續損益計算的基礎。
     */
    private void upsertBuyPosition(Long userId, String symbol, int qty, BigDecimal price) {
        PositionEntity pos = positionRepository.findByUserIdAndSymbol(userId, symbol).orElse(null);
        if (pos == null) {
            pos = new PositionEntity();
            pos.setUserId(userId);
            pos.setSymbol(symbol);
            pos.setQuantity(qty);
            pos.setAvgPrice(price);
        } else {
            int newQty = pos.getQuantity() + qty;
            BigDecimal oldCost = pos.getAvgPrice().multiply(BigDecimal.valueOf(pos.getQuantity()));
            BigDecimal addCost = price.multiply(BigDecimal.valueOf(qty));
            BigDecimal avg = oldCost.add(addCost)
                    .divide(BigDecimal.valueOf(newQty), 4, RoundingMode.HALF_UP);
            pos.setQuantity(newQty);
            pos.setAvgPrice(avg);
        }
        positionRepository.save(pos);
    }

    /**
     * 【職責】驗證帳本入帳所需的使用者、商品、方向、數量與價格。
     * 【技巧】對空值、空白字串與非正數及早拋出 BusinessException。
     * 【概念】交易資料的完整性應在更新帳本前被強制保證。
     */
    private void validateTrade(ApplyTradeRequest req) {
        if (req.getUserId() == null || req.getSymbol() == null || req.getSymbol().isBlank()) {
            throw new BusinessException("userId and symbol required");
        }
        if (req.getSide() == null || req.getSide().isBlank()) {
            throw new BusinessException("side required");
        }
        if (req.getQuantity() <= 0) {
            throw new BusinessException("quantity must be positive");
        }
        if (req.getPrice() == null || req.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("price must be positive");
        }
    }

    /**
     * 【職責】取得交易名義金額，缺省時依價格乘數量計算。
     * 【技巧】優先採用事件攜帶的 notional，避免不同服務重算規則產生差異。
     * 【概念】明確傳遞金額可維持跨服務事件的財務語意一致。
     */
    private BigDecimal resolveNotional(ApplyTradeRequest req) {
        if (req.getNotional() != null) {
            return req.getNotional();
        }
        return req.getPrice().multiply(BigDecimal.valueOf(req.getQuantity()));
    }

    private AccountDto toAccountDto(AccountEntity a) {
        return new AccountDto(a.getUserId(), a.getCashBalance(), a.getCurrency());
    }

    private PositionDto toPositionDto(PositionEntity p) {
        return new PositionDto(p.getSymbol(), p.getQuantity(), p.getAvgPrice());
    }
}
