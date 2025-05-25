package com.project200.undabang.exercise.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CreateExerciseRequestDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("종료일시가 시작일시 이전이면 유효성 검사 실패")
    void validationFailureWhenEndTimeBeforeStartTime() {
        // given
        CreateExerciseRequestDto dto = CreateExerciseRequestDto.builder()
                .exerciseTitle("운동 제목")
                .exercisePersonalType("개인")
                .exerciseLocation("운동 장소")
                .exerciseDetail("운동 상세")
                .exerciseStartedAt(LocalDateTime.now().minusDays(1))
                .exerciseEndedAt(LocalDateTime.now().minusDays(2))
                .build();

        // when
        Set<ConstraintViolation<CreateExerciseRequestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).as("종료시간이 시작시간 이전인 경우 유효성 검사 실패").isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).as("오류 메시지 확인")
                .isEqualTo("종료 일시는 시작 일시 이후여야 합니다.");
    }

    @Test
    @DisplayName("종료시간이 시작시간 이후면 유효성 검사 성공")
    void ValidationSuccessWhenEndTimeAfterStartTime() {
        // given
        CreateExerciseRequestDto dto = CreateExerciseRequestDto.builder()
                .exerciseTitle("운동 제목")
                .exercisePersonalType("개인")
                .exerciseLocation("운동 장소")
                .exerciseDetail("운동 상세")
                .exerciseStartedAt(LocalDateTime.now().minusDays(2))
                .exerciseEndedAt(LocalDateTime.now().minusDays(1))
                .build();

        // when
        Set<ConstraintViolation<CreateExerciseRequestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).as("종료시간이 시작시간 이후인 경우 유효성 검사 성공")
                .isEmpty();
    }

    @Test
    @DisplayName("종료 시간이 미래면 유효성 검사 실패")
    void validationFailureWhenEndTimeIsFuture() {
        // given
        CreateExerciseRequestDto dto = CreateExerciseRequestDto.builder()
                .exerciseTitle("운동 제목")
                .exercisePersonalType("개인")
                .exerciseLocation("운동 장소")
                .exerciseDetail("운동 상세")
                .exerciseStartedAt(LocalDateTime.now().minusDays(1))
                .exerciseEndedAt(LocalDateTime.now().plusDays(1)) // 미래 날짜
                .build();

        // when
        Set<ConstraintViolation<CreateExerciseRequestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).as("종료 시간이 미래인 경우 유효성 검사 실패").isNotEmpty();
        boolean hasPastViolation = violations.stream()
                .anyMatch(violation -> violation.getMessage().equals("종료 일시는 현재 시간 이전이어야 합니다."));
        assertThat(hasPastViolation).as("종료 시간이 현재 시간 이전이어야 한다는 오류 메시지 확인").isTrue();
    }

    @Test
    @DisplayName("시작 시간이 미래면 유효성 검사 실패")
    void validationFailureWhenStartTimeIsFuture() {
        // given
        CreateExerciseRequestDto dto = CreateExerciseRequestDto.builder()
                .exerciseTitle("운동 제목")
                .exercisePersonalType("개인")
                .exerciseLocation("운동 장소")
                .exerciseDetail("운동 상세")
                .exerciseStartedAt(LocalDateTime.now().plusDays(1)) // 미래 날짜
                .exerciseEndedAt(LocalDateTime.now().plusDays(2))
                .build();

        // when
        Set<ConstraintViolation<CreateExerciseRequestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).as("시작 시간이 미래인 경우 유효성 검사 실패").isNotEmpty();
        boolean hasPastViolation = violations.stream()
                .anyMatch(violation -> violation.getMessage().equals("시작 일시는 현재 시간 이전이어야 합니다."));
        assertThat(hasPastViolation)
                .as("시작 시간이 현재 시간 이전이어야 한다는 오류 메시지 확인")
                .isTrue();
    }
}