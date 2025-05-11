package com.project200.undabang.common.web.advice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 이 어노테이션이 붙은 컨트롤러/메소드는 ApiResponse 래핑을 건너뜁니다.
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@interface NoApiResponseWrapper {
}