package com.project200.undabang.common.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)// 어노테이션의 정보가 런타임 시점까지 유지되도록 설정
public @interface LogExecutionTime {
}
