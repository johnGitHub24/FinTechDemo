package com.fintech.demo.account.common;

/**
 * 【職責】資源不存在（帳戶／持倉查無）。
 * 【技巧】由 GlobalExceptionHandler 映成 HTTP 狀態。
 * 【概念】用例外類型區分 404／400，比回傳 null 更明確。
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
