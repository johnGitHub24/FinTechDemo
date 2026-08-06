package com.fintech.demo.order.common;

/**
 * 【職責】業務規則衝突（餘額不足、重複 clientOrderId、不可取消等）。
 * 【技巧】由 GlobalExceptionHandler 映成 HTTP 狀態。
 * 【概念】用例外類型區分 404／400，比回傳 null 更明確。
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
