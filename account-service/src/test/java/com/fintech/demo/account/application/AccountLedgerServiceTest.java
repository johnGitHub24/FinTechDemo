package com.fintech.demo.account.application;

import com.fintech.demo.account.common.BusinessException;
import com.fintech.demo.account.infrastructure.AccountEntity;
import com.fintech.demo.account.infrastructure.AccountRepository;
import com.fintech.demo.account.infrastructure.PositionEntity;
import com.fintech.demo.account.infrastructure.PositionRepository;
import com.fintech.demo.common.dto.AccountDto;
import com.fintech.demo.common.dto.ApplyTradeRequest;
import com.fintech.demo.common.dto.PositionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
/**
 * 【職責】驗證帳本服務的買賣入帳、現金餘額與持倉轉換規則。
 * 【技巧】以 Mockito 隔離 Repository，並以 ArgumentCaptor 檢查寫入的持倉內容。
 * 【概念】帳本是現金與持倉的唯一寫入點，測試保護其金融不變量。
 */
class AccountLedgerServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PositionRepository positionRepository;

    private AccountLedgerService service;

    @BeforeEach
    void setUp() {
        service = new AccountLedgerService(accountRepository, positionRepository);
    }

    /**
     * CASE LEDGER-001：Given 現金足夠且沒有既有持倉，When 買進入帳，Then 扣現金並建立持倉。
     */
    @Test
    void buy_shouldDeductCashAndCreatePosition() {
        AccountEntity account = account(1L, "85000.00");
        when(accountRepository.findByUserId(1L)).thenReturn(Optional.of(account));
        when(positionRepository.findByUserIdAndSymbol(1L, "AAPL")).thenReturn(Optional.empty());
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(positionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AccountDto result = service.applyTrade(buy(1L, "AAPL", 10, "150.00"));

        assertThat(result.cashBalance()).isEqualByComparingTo("83500.00");
        ArgumentCaptor<PositionEntity> cap = ArgumentCaptor.forClass(PositionEntity.class);
        verify(positionRepository).save(cap.capture());
        assertThat(cap.getValue().getQuantity()).isEqualTo(10);
        assertThat(cap.getValue().getAvgPrice()).isEqualByComparingTo("150.00");
    }

    /**
     * CASE LEDGER-002：Given 足額既有持倉，When 賣出入帳，Then 減少持倉並增加現金。
     */
    @Test
    void sell_shouldReducePositionAndAddCash() {
        AccountEntity account = account(1L, "85000.00");
        PositionEntity pos = position(1L, "AAPL", 100, "150.00");
        when(accountRepository.findByUserId(1L)).thenReturn(Optional.of(account));
        when(positionRepository.findByUserIdAndSymbol(1L, "AAPL")).thenReturn(Optional.of(pos));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(positionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AccountDto result = service.applyTrade(sell(1L, "AAPL", 10, "160.00"));

        assertThat(result.cashBalance()).isEqualByComparingTo("86600.00");
        assertThat(pos.getQuantity()).isEqualTo(90);
    }

    /**
     * CASE LEDGER-003：Given 現金不足，When 買進入帳，Then 拋出商業例外。
     */
    @Test
    void buy_insufficientCash_shouldFail() {
        when(accountRepository.findByUserId(1L)).thenReturn(Optional.of(account(1L, "100.00")));
        assertThatThrownBy(() -> service.applyTrade(buy(1L, "AAPL", 10, "150.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("insufficient cash");
    }

    /**
     * CASE LEDGER-004：Given Repository 回傳持倉實體，When 查詢持倉，Then 映射為 DTO。
     */
    @Test
    void LEDGER_004_listPositions_shouldMapEntities() {
        when(positionRepository.findByUserId(1L)).thenReturn(List.of(position(1L, "AAPL", 100, "150")));
        List<PositionDto> list = service.listPositions(1L);
        assertThat(list).hasSize(1);
        assertThat(list.getFirst().symbol()).isEqualTo("AAPL");
    }

    /**
     * CASE ACCOUNT-001：Given 種子帳戶，When getAccount，Then 回傳現金 85000。
     */
    @Test
    void ACCOUNT_001_getAccount_shouldMapSeedCash() {
        when(accountRepository.findByUserId(1L)).thenReturn(Optional.of(account(1L, "85000.00")));
        AccountDto dto = service.getAccount(1L);
        assertThat(dto.userId()).isEqualTo(1L);
        assertThat(dto.cashBalance()).isEqualByComparingTo("85000.00");
    }

    private static AccountEntity account(Long userId, String cash) {
        AccountEntity a = new AccountEntity();
        a.setUserId(userId);
        a.setCashBalance(new BigDecimal(cash));
        a.setCurrency("TWD");
        return a;
    }

    private static PositionEntity position(Long userId, String symbol, int qty, String avg) {
        PositionEntity p = new PositionEntity();
        p.setUserId(userId);
        p.setSymbol(symbol);
        p.setQuantity(qty);
        p.setAvgPrice(new BigDecimal(avg));
        return p;
    }

    private static ApplyTradeRequest buy(Long userId, String symbol, int qty, String price) {
        ApplyTradeRequest r = new ApplyTradeRequest();
        r.setOrderId(99L);
        r.setUserId(userId);
        r.setSymbol(symbol);
        r.setSide("BUY");
        r.setQuantity(qty);
        r.setPrice(new BigDecimal(price));
        r.setNotional(new BigDecimal(price).multiply(BigDecimal.valueOf(qty)));
        return r;
    }

    private static ApplyTradeRequest sell(Long userId, String symbol, int qty, String price) {
        ApplyTradeRequest r = buy(userId, symbol, qty, price);
        r.setSide("SELL");
        return r;
    }
}
