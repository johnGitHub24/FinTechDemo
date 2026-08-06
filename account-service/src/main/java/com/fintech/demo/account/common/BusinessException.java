package com.fintech.demo.account.common;

/**
 * 【職責】業務規則違規（現金不足、空倉賣出等）。
 * 【技巧】由 GlobalExceptionHandler 映成 HTTP 狀態。
 * 【概念】與 HTTP 層分離：Service 拋出，Controller／Advice 轉 400／422。
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
