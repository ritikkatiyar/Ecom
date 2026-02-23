package com.ecom.common.web;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

/**
 * Logs when each API (controller method) is invoked.
 * Wraps all @RestController methods to log "API invoked: ControllerName#methodName".
 * Registered via RequestLoggingFilterConfig (auto-configuration).
 */
@Aspect
@Order(1)
public class ApiInvocationLoggingAspect {

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object logApiInvocation(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Logger log = LoggerFactory.getLogger(joinPoint.getTarget().getClass());
        log.info("API invoked: {}#{}", className, methodName);
        return joinPoint.proceed();
    }
}
