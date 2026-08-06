package com.fintech.demo.order.config;

import com.fintech.demo.order.domain.OrderSide;
import com.fintech.demo.order.domain.OrderStatus;
import com.fintech.demo.order.domain.Role;
import com.fintech.demo.order.infrastructure.AccountEntity;
import com.fintech.demo.order.infrastructure.AccountRepository;
import com.fintech.demo.order.infrastructure.AuditLogEntity;
import com.fintech.demo.order.infrastructure.AuditLogRepository;
import com.fintech.demo.order.infrastructure.OrderEntity;
import com.fintech.demo.order.infrastructure.OrderRepository;
import com.fintech.demo.order.infrastructure.PositionEntity;
import com.fintech.demo.order.infrastructure.PositionRepository;
import com.fintech.demo.order.infrastructure.UserEntity;
import com.fintech.demo.order.infrastructure.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 【職責】寫入串連假資料：users → accounts／orders／positions／audit_log。
 * 【技巧】密碼以 BCrypt 寫入，供 JWT 登入驗證。
 * 【概念】教學 Demo 以可講清邊界為優先。
 */
@Component
public class DemoDataSeeder implements ApplicationRunner {

    public static final String TRADER1 = "trader1";
    public static final String ADMIN = "admin";
    public static final String DEMO_PASSWORD = "password";

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final OrderRepository orderRepository;
    private final PositionRepository positionRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(
            UserRepository userRepository,
            AccountRepository accountRepository,
            OrderRepository orderRepository,
            PositionRepository positionRepository,
            AuditLogRepository auditLogRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.orderRepository = orderRepository;
        this.positionRepository = positionRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 【職責】在空資料庫建立可登入的使用者、帳戶、訂單、持倉與審計範例。
     * 【技巧】以 userRepository.count 作冪等 guard，所有種子資料在同一交易中寫入。
     * 【概念】一致的 Demo 資料可直接展示登入、下單、查帳與審計整條流程。
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }

        UserEntity trader = saveUser(TRADER1, Role.USER);
        UserEntity adminUser = saveUser(ADMIN, Role.ADMIN);

        // 初始 100000；已成交 BUY AAPL 100@150 = 15000 → 餘額 85000
        saveAccount(trader.getId(), new BigDecimal("85000.00"));
        saveAccount(adminUser.getId(), new BigDecimal("100000.00"));

        OrderEntity filled = saveOrder(trader.getId(), "SEED-AAPL-BUY-1", "AAPL",
                OrderSide.BUY, 100, "150.0000", OrderStatus.ACCEPTED);
        OrderEntity pending = saveOrder(trader.getId(), "SEED-AAPL-SELL-1", "AAPL",
                OrderSide.SELL, 10, "160.0000", OrderStatus.PENDING);
        OrderEntity cancelled = saveOrder(trader.getId(), "SEED-TSLA-BUY-1", "TSLA",
                OrderSide.BUY, 5, "200.0000", OrderStatus.CANCELLED);
        saveOrder(adminUser.getId(), "SEED-ADMIN-MSFT-1", "MSFT",
                OrderSide.BUY, 20, "300.0000", OrderStatus.PENDING);

        PositionEntity pos = new PositionEntity();
        pos.setUserId(trader.getId());
        pos.setSymbol("AAPL");
        pos.setQuantity(100);
        pos.setAvgPrice(new BigDecimal("150.0000"));
        positionRepository.save(pos);

        audit(TRADER1, "ORDER_ACCEPTED", "orders/" + filled.getId(),
                "BUY AAPL 100@150 → cash 85000, position AAPL 100");
        audit(TRADER1, "ORDER_CREATED", "orders/" + pending.getId(),
                "PENDING SELL AAPL 10@160");
        audit(TRADER1, "ORDER_CANCELLED", "orders/" + cancelled.getId(),
                "CANCELLED BUY TSLA 5@200");
        audit(ADMIN, "ORDER_CREATED", "orders", "ADMIN seeded PENDING MSFT");
    }

    /**
     * 【職責】建立並保存指定角色的展示使用者。
     * 【技巧】以共用 PasswordEncoder 對固定 Demo 密碼雜湊後再保存。
     * 【概念】種子資料也應遵守正式登入使用的密碼存放規則。
     */
    private UserEntity saveUser(String username, Role role) {
        UserEntity u = new UserEntity();
        u.setUsername(username);
        u.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        u.setRole(role);
        return userRepository.save(u);
    }

    /**
     * 【職責】為展示使用者建立 TWD 現金帳戶。
     * 【技巧】帳戶資料由 userId 關聯，不將展示名稱寫入帳本。
     * 【概念】帳戶是訂單成交扣款與風控現金檢查的資料基礎。
     */
    private void saveAccount(Long userId, BigDecimal cash) {
        AccountEntity a = new AccountEntity();
        a.setUserId(userId);
        a.setCashBalance(cash);
        a.setCurrency("TWD");
        accountRepository.save(a);
    }

    /**
     * 【職責】建立並保存具指定狀態的展示訂單。
     * 【技巧】集中設定冪等鍵、商品、方向、價格與數量，避免 seed 欄位不一致。
     * 【概念】預置多種狀態能讓 Portal 直接展示訂單狀態機。
     */
    private OrderEntity saveOrder(
            Long userId, String clientOrderId, String symbol,
            OrderSide side, int qty, String price, OrderStatus status) {
        OrderEntity o = new OrderEntity();
        o.setUserId(userId);
        o.setClientOrderId(clientOrderId);
        o.setSymbol(symbol);
        o.setSide(side);
        o.setQuantity(qty);
        o.setPrice(new BigDecimal(price));
        o.setStatus(status);
        return orderRepository.save(o);
    }

    /**
     * 【職責】寫入展示用審計紀錄。
     * 【技巧】以動作、資源與細節組成可查詢的 audit_log 資料。
     * 【概念】審計紀錄讓系統能回答誰在何時執行了哪項交易操作。
     */
    private void audit(String username, String action, String resource, String detail) {
        AuditLogEntity log = new AuditLogEntity();
        log.setUsername(username);
        log.setAction(action);
        log.setResource(resource);
        log.setDetail(detail);
        auditLogRepository.save(log);
    }
}
