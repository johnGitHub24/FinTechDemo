package com.fintech.demo.order.dto;

import java.util.List;

/**
 * 【職責】登入成功回應：JWT、使用者與角色。
 * 【技巧】Java {@code record}；三參建構子預設 {@code tokenType=Bearer}。
 * 【概念】為何用 record？登入結果是一次性發行的不可變契約；之後靠 JWT，伺服器不需 session。
 *         對照：{@link LoginRequest} 需 Jackson {@code setXxx} → class + Lombok {@code @Data}。
 */
public record LoginResponse(String token, String tokenType, String username, List<String> roles) {

    /** 【技巧】record 多載建構子委派全參建構子。 */
    public LoginResponse(String token, String username, List<String> roles) {
        this(token, "Bearer", username, roles);
    }
}
