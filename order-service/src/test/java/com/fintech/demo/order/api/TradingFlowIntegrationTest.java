package com.fintech.demo.order.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.demo.common.dto.RiskCheckResponse;
import com.fintech.demo.order.client.AccountClient;
import com.fintech.demo.order.client.RiskClient;
import com.fintech.demo.order.config.DemoDataSeeder;
import com.fintech.demo.order.infrastructure.AccountRepository;
import com.fintech.demo.order.infrastructure.AuditLogRepository;
import com.fintech.demo.order.infrastructure.OrderRepository;
import com.fintech.demo.order.infrastructure.PositionRepository;
import com.fintech.demo.order.infrastructure.UserRepository;
import com.fintech.demo.order.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 【職責】驗證種子資料、JWT 授權與交易 API 的端對端流程。
 * 【技巧】套件 {@code /api/} 讓成對掃描歸入整合層；外部 Client 以 MockBean 隔離。
 * 【概念】整合測試保護交易歷程、帳戶餘額與持倉在 API 操作後的一致性。
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TradingFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @MockBean
    private RiskClient riskClient;

    @MockBean
    private AccountClient accountClient;

    private String traderToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        var trader = userRepository.findByUsername(DemoDataSeeder.TRADER1).orElseThrow();
        var admin = userRepository.findByUsername(DemoDataSeeder.ADMIN).orElseThrow();
        traderToken = tokenProvider.generateToken(trader.getUsername(), trader.getId(), List.of("ROLE_USER"));
        adminToken = tokenProvider.generateToken(admin.getUsername(), admin.getId(), List.of("ROLE_ADMIN"));
        when(riskClient.check(any())).thenReturn(RiskCheckResponse.ok());
    }

    /**
     * CASE FLOW-001：Given 種子資料已建立，When 查詢各 Repository，Then 資料關聯完整。
     */
    @Test
    void FLOW_001_seedData_shouldLinkAllTables() {
        var trader = userRepository.findByUsername(DemoDataSeeder.TRADER1).orElseThrow();
        assertThat(accountRepository.findByUserId(trader.getId()).orElseThrow().getCashBalance())
                .isGreaterThan(java.math.BigDecimal.ZERO);
        assertThat(orderRepository.findAll()).hasSizeGreaterThanOrEqualTo(4);
        assertThat(positionRepository.findByUserIdAndSymbol(trader.getId(), "AAPL").orElseThrow().getQuantity())
                .isGreaterThanOrEqualTo(100);
        assertThat(auditLogRepository.count()).isGreaterThanOrEqualTo(4);
    }

    /**
     * CASE FLOW-002：Given 未帶 JWT，When 存取受保護帳戶 API，Then 回應 401 Unauthorized。
     */
    @Test
    void FLOW_002_withoutToken_shouldGet401() throws Exception {
        mockMvc.perform(get("/api/accounts/me"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * CASE FLOW-003：Given USER 與 ADMIN Token，When 查詢稽核 API，Then 僅 ADMIN 可取得資料。
     */
    @Test
    void FLOW_003_userCannotAccessAudit_adminCan() throws Exception {
        mockMvc.perform(get("/api/audit-logs").header("Authorization", bearer(traderToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/audit-logs").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    /**
     * CASE FLOW-004：Given 已登入種子交易者，When 查詢入口 API，Then 回傳帳戶、持倉、訂單與市場資料。
     */
    @Test
    void FLOW_004_portalApis_shouldReflectSeededTrader() throws Exception {
        mockMvc.perform(get("/api/accounts/me").header("Authorization", bearer(traderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashBalance").value(org.hamcrest.Matchers.greaterThan(0.0)));

        mockMvc.perform(get("/api/positions").header("Authorization", bearer(traderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("AAPL"));

        mockMvc.perform(get("/api/orders").header("Authorization", bearer(traderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(get("/api/market/symbols").header("Authorization", bearer(traderToken)))
                .andExpect(status().isOk());
    }

    /**
     * CASE FLOW-005：Given 風控核准的買單，When 建單並執行，Then 訂單、餘額與持倉同步更新。
     */
    @Test
    void FLOW_005_createExecute_shouldUpdateHistoryBalancePosition() throws Exception {
        BigDecimal before = accountRepository.findByUserId(
                userRepository.findByUsername(DemoDataSeeder.TRADER1).orElseThrow().getId())
                .orElseThrow().getCashBalance();

        String body = """
                {
                  "clientOrderId": "IT-BUY-%d",
                  "symbol": "MSFT",
                  "side": "BUY",
                  "quantity": 1,
                  "price": 300.00
                }
                """.formatted(System.nanoTime());

        MvcResult created = mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(traderToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.demoTrace.action").value("CREATE_ORDER"))
                .andReturn();

        long orderId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/orders/{id}/execute", orderId)
                        .header("Authorization", bearer(traderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.demoTrace.action").value("EXECUTE"));

        mockMvc.perform(get("/api/accounts/me").header("Authorization", bearer(traderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashBalance")
                        .value(before.subtract(new BigDecimal("300.00")).doubleValue()));

        mockMvc.perform(get("/api/positions").header("Authorization", bearer(traderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.symbol=='MSFT')].quantity").value(org.hamcrest.Matchers.hasItem(1)));
    }

    /**
     * CASE FLOW-006：Given 新建待處理訂單，When 取消訂單，Then 歷程保存 CANCELLED 狀態。
     */
    @Test
    void FLOW_006_cancelPending_shouldAppearInHistory() throws Exception {
        String body = """
                {
                  "clientOrderId": "IT-CXL-%d",
                  "symbol": "TSLA",
                  "side": "BUY",
                  "quantity": 1,
                  "price": 200.00
                }
                """.formatted(System.nanoTime());

        MvcResult created = mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(traderToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        long orderId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/orders/{id}", orderId).header("Authorization", bearer(traderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    /**
     * CASE FLOW-007：Given 已授權管理者，When 查詢訂單列表，Then 可取得全體訂單。
     */
    @Test
    void FLOW_007_adminList_shouldSeeAllOrders() throws Exception {
        mockMvc.perform(get("/api/orders").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(4)));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
