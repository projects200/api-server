package com.project200.undabang.common.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ExecutionTimeLoggerAspect {

    @Around("@annotation(com.project200.undabang.common.aop.LogExecutionTime)")
    public Object logExecutionTime(final ProceedingJoinPoint joinPoint) throws Throwable {

        // 대상 메소드 실행 전 시작 시간 측정
        long startTime = System.currentTimeMillis();

        // 대상 메소드 실행
        Object result = joinPoint.proceed();

        // 대상 메소드 실행 수 종료 시간 측정
        long endTime = System.currentTimeMillis();

        long duration = endTime - startTime;
        // 로그 출력
        String methodName = joinPoint.getSignature().getName();
        log.info("[ExecutionTime] '{}' executed in {} ms", methodName, duration);

        // 원래 메소드의 값을 그대로 반환.
        // 이 값을 반환하지 않으면 null을 받게 됨
        return result;
    }
}
