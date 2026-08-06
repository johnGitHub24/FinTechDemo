package com.fintech.demo.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 【職責】登入請求：username／password。
 * 【技巧】class + Lombok {@code @Data}（非 record）：Jackson 綁定需 {@code setXxx}；{@code @NotBlank} 擋空字串。
 * 【概念】為何不用 record？Request 需可變綁定；登入成功後的不可變結果見 {@link LoginResponse} record。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
}
