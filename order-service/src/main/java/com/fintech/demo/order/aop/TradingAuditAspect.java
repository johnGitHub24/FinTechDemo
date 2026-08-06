package com.fintech.demo.order.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 【職責】AOP 觀測交易成功路徑（對齊 TradingIocAOP；audit_log 仍由 Service 落地）。
 * 【技巧】配合同套件 Service／Controller 使用。
 * 【概念】教學 Demo 以可講清邊界為優先。
 */
@Aspect
@Component
@Order(60)
public class TradingAuditAspect {

    private static final Logger log = LoggerFactory.getLogger(TradingAuditAspect.class);

    /**
     * 【職責】記錄標示 @Audited 方法成功返回時的觀測日誌。
     * 【技巧】以 annotation pointcut 取得動作代碼與方法簽名，不侵入業務方法。
     * 【概念】AOP 適合處理紀錄等橫切關注點，讓交易流程保持聚焦。
     */
    @AfterReturning(pointcut = "@annotation(audited)", returning = "result")
    public void afterSuccess(JoinPoint joinPoint, Audited audited, Object result) {
        log.info("AOP @Audited action={} method={} resultType={}",
                audited.action(),
                joinPoint.getSignature().toShortString(),
                result == null ? "null" : result.getClass().getSimpleName());
    }
}
