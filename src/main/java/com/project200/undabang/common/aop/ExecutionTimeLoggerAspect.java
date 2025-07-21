package com.project200.undabang.common.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


/**
 * 메서드 실행 시간을 측정하고 기록하는 AOP Aspect입니다.
 * {@code @LogExecutionTime} 어노테이션이 붙은 메서드를 대상으로 합니다.
 * 다른 Aspect들보다 먼저 실행되어야 정확한 시간 측정이 가능하므로 가장 높은 우선순위를 가집니다.
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ExecutionTimeLoggerAspect {

    /**
     * {@code @LogExecutionTime} 어노테이션이 적용된 메서드의 실행을 가로채는 Around 어드바이스입니다.
     * 메서드 실행 시작 전후의 시간을 측정하여 실행 시간을 계산하고 로그로 남깁니다.
     *
     * @param joinPoint 프록시된 메서드에 대한 정보를 제공합니다.
     * @return 원본 메서드의 실행 결과를 반환합니다.
     * @throws Throwable 원본 메서드에서 발생한 예외를 전파합니다.
     */
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
