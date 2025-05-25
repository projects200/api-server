package com.project200.undabang.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapperImpl;

import java.time.LocalDateTime;

public class StartBeforeEndValidator implements ConstraintValidator<StartBeforeEnd, Object> {

    private String startTimeFieldName;
    private String endTimeFieldName;

    @Override
    public void initialize(StartBeforeEnd constraintAnnotation) {
        this.startTimeFieldName = constraintAnnotation.startTimeFieldName();
        this.endTimeFieldName = constraintAnnotation.endTimeFieldName();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        Object startTimeValue = new BeanWrapperImpl(value).getPropertyValue(startTimeFieldName);
        Object endTimeValue = new BeanWrapperImpl(value).getPropertyValue(endTimeFieldName);

        if (startTimeValue == null || endTimeValue == null) {
            return true; // 다른 @NotNull 어노테이션에서 처리하도록 함
        }

        if (!(startTimeValue instanceof LocalDateTime startTime) || !(endTimeValue instanceof LocalDateTime endTime)) {
            throw new IllegalArgumentException("시작 시간과 종료 시간은 LocalDateTime 타입이어야 합니다.");
        }

        return startTime.isBefore(endTime) || startTime.isEqual(endTime);
    }
}