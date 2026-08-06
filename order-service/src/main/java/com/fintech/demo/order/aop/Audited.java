package com.fintech.demo.order.aop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 【職責】標記需要寫入審計日誌的業務方法。
 * 【技巧】由 {@link TradingAuditAspect} 攔截；{@code action} 寫入 audit 表。
 * 【概念】AOP 把「記一筆誰做了什麼」橫切出去，避免每個 Service 手動重複。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Audited {
    /** 審計動作代碼，例如 ORDER_CREATED／ORDER_EXECUTE。 */
    String action();
}
