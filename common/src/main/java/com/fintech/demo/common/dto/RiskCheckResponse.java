package com.fintech.demo.common.dto;

/**
 * 【職責】風控檢查結果。
 * 【技巧】Java {@code record} + 靜態工廠 {@code ok()}／{@code reject()}；布林存取為 {@code allowed()}。
 * 【概念】為何用 record？決策結果是不可變值物件（契約／回應），組裝後不應被改寫。
 *         對照：{@link RiskCheckRequest} 需 setter 組裝 → class + Lombok。
 */
public record RiskCheckResponse(boolean allowed, String reason) {

    /** 【職責】風控通過的標準結果。 */
    public static RiskCheckResponse ok() {
        return new RiskCheckResponse(true, "OK");
    }

    /** 【職責】帶拒絕原因的風控結果。 */
    public static RiskCheckResponse reject(String reason) {
        return new RiskCheckResponse(false, reason);
    }
}
