package com.fintech.demo.risk.application;

import com.fintech.demo.common.dto.RiskCheckRequest;
import com.fintech.demo.common.dto.RiskCheckResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 【職責】精簡風控：名義金額 ≤ 現金，且 ≤ 單筆上限。
 * 【技巧】上限來自 {@code fintech.risk.max-notional}；純記憶體規則、無 DB。
 * 【概念】獨立 risk-service 後，規則變更不必重佈署 order-service。
 */
@Service
public class RiskService {

    private final BigDecimal maxNotional;

    public RiskService(@Value("${fintech.risk.max-notional:50000}") BigDecimal maxNotional) {
        this.maxNotional = maxNotional;
    }

    /**
     * 【職責】檢查單筆名義金額是否通過風控。
     * 【技巧】規則：數量／價格合法 → notional ≤ maxNotional → BUY 時 notional ≤ 現金。
     * 【概念】風控獨立進程後，order 只能「問可不可以」，不能自己偷偷改規則。
     * @param request 含 side／qty／price／cashBalance
     * @return allowed + reason
     */
    public RiskCheckResponse check(RiskCheckRequest request) {
        // 細節：先擋非法輸入，避免後續 BigDecimal 運算無意義
        if (request.getQuantity() == null || request.getQuantity() <= 0
                || request.getPrice() == null || request.getPrice().signum() <= 0) {
            return RiskCheckResponse.reject("invalid quantity or price");
        }
        BigDecimal notional = request.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
        // 細節：單筆上限（預設 50000）防止 Demo 一次打爆帳本
        if (notional.compareTo(maxNotional) > 0) {
            return RiskCheckResponse.reject("notional exceeds max " + maxNotional);
        }
        // 細節：賣出不檢查現金；買入必須現金足夠覆蓋名義金額
        if ("BUY".equalsIgnoreCase(request.getSide())) {
            if (request.getCashBalance() == null || request.getCashBalance().compareTo(notional) < 0) {
                return RiskCheckResponse.reject("insufficient cash for notional " + notional);
            }
        }
        return RiskCheckResponse.ok();
    }
}
