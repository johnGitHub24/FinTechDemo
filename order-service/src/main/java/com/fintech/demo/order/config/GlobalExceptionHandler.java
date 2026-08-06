package com.fintech.demo.order.config;

import com.fintech.demo.order.common.BusinessException;
import com.fintech.demo.order.common.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 【職責】統一 API 錯誤回應。
 * 【技巧】由 GlobalExceptionHandler 映成 HTTP 狀態。
 * 【概念】用例外類型區分 404／400，比回傳 null 更明確。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 【職責】將找不到資源的業務例外轉為 404 回應。
     * 【技巧】保留例外訊息於統一 error payload。
     * 【概念】HTTP 狀態碼把資源不存在與輸入錯誤清楚區分。
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * 【職責】將可預期的業務規則違反轉為 422 回應。
     * 【技巧】使用 Unprocessable Entity 表示 JSON 格式正確但不符合交易規則。
     * 【概念】例外到 HTTP 的映射讓 Service 可專注業務語意。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> business(BusinessException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * 【職責】將帳密失敗統一轉為不洩漏帳號存在性的 401 回應。
     * 【技巧】固定錯誤文字，不回傳底層驗證細節。
     * 【概念】登入錯誤應避免提供可用於帳號枚舉的資訊。
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> badCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "invalid username or password"));
    }

    /**
     * 【職責】將 Bean Validation 失敗轉為可讀的 400 回應。
     * 【技巧】取第一個欄位錯誤組成精簡 error payload。
     * 【概念】輸入格式驗證在進入業務服務前就應被拒絕。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .orElse("validation failed");
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }

    /**
     * 【職責】將不合法參數轉為 400 回應。
     * 【技巧】沿用統一 Map error 契約。
     * 【概念】一致錯誤模型可降低前端處理各端點失敗的複雜度。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
