package com.project200.undabang.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = AllowedExtensionsValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedExtensions {
    String message() default "허용되지 않은 파일 확장자입니다.";
    String[] extensions();
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
