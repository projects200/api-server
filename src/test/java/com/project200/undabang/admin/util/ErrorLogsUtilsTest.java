package com.project200.undabang.admin.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

import static org.junit.jupiter.api.Assertions.*;

class ErrorLogsUtilsTest {

    // 테스트용 예외를 생성하는 헬퍼 메소드
    private Exception createExceptionFromInsidePackage() {
        // 이 테스트 클래스는 com.project200.undabang 패키지 내에 있으므로,
        // 여기서 발생한 예외는 findClassErrorHappened 메소드의 탐지 대상이 됩니다.
        try {
            throw new IllegalStateException("테스트 예외 발생");
        } catch (IllegalStateException e) {
            return e;
        }
    }

    @Nested
    @DisplayName("findClassErrorHappened 메소드 테스트")
    class FindClassErrorHappenedTest{

        @Test
        @DisplayName("null 입력 시 특정 문자열을 반환해야 한다")
        void givenNull_whenFindClassErrorHappened_thenReturnsSpecificString() {
            // when
            String result = ErrorLogsUtils.findClassErrorHappened(null);

            // then
            assertEquals("throwable 값이 없습니다.", result);
        }

        @Test
        @DisplayName("프로젝트 내부에서 발생한 예외의 위치를 정확히 찾아야 한다")
        void givenExceptionFromInside_whenFindClassErrorHappened_thenFindsCorrectClass() {
            // given
            Exception exception = createExceptionFromInsidePackage();

            // when
            String result = ErrorLogsUtils.findClassErrorHappened(exception);

            // then
            assertTrue(result.contains("com.project200.undabang.admin.util.ErrorLogsUtilsTest.createExceptionFromInsidePackage"));
        }
    }

    @Nested
    @DisplayName("getStructuredStackTrace 메소드 테스트")
    class GetStructuredStackTraceTest {

        @Test
        @DisplayName("null 입력 시 특정 문자열을 반환해야 한다")
        void givenNull_whenGetStructuredStackTrace_thenReturnsSpecificString() {
            // when
            String result = ErrorLogsUtils.getStructuredStackTrace(null);

            // then
            assertEquals("throwable 값이 없습니다.", result);
        }

        @Test
        @DisplayName("예외 발생 시 구조화된 스택 트레이스를 반환해야 한다")
        void givenException_whenGetStructuredStackTrace_thenReturnsStructuredString() {
            // given
            Exception rootCause = new IllegalStateException("Root Cause");
            Exception exception = new RuntimeException("Wrapper Exception", rootCause);

            // when
            String result = ErrorLogsUtils.getStructuredStackTrace(exception);

            // then
            assertAll(
                    () -> assertTrue(result.contains("*RootCause*")),
                    () -> assertTrue(result.contains("java.lang.IllegalStateException: Root Cause"))
            );
        }

        @Test
        @DisplayName("스택 트레이스가 MAX_LINE_LIMIT를 초과하면 축약되어야 한다")
        void givenLongStackTrace_whenGetStructuredStackTrace_thenIsTruncated() {
            // given
            Exception exception = new IllegalStateException("Long Stack Trace");
            StackTraceElement[] stackTrace = {
                    new StackTraceElement("com.project200.undabang.service.A", "methodA", "A.java", 1),
                    new StackTraceElement("com.project200.undabang.service.B", "methodB", "B.java", 1),
                    new StackTraceElement("com.project200.undabang.service.C", "methodC", "C.java", 1),
                    new StackTraceElement("com.project200.undabang.service.D", "methodD", "D.java", 1), // 4번째 내부 요소
                    new StackTraceElement("org.external.E", "methodE", "E.java", 1)
            };
            exception.setStackTrace(stackTrace);

            // when
            String result = ErrorLogsUtils.getStructuredStackTrace(exception);

            // then
            // MAX_LINE_LIMIT(3)을 초과했으므로, 축약 문자열 "..."이 포함되어야 합니다.
            assertTrue(result.contains("..."));
        }
    }

    @Nested
    @DisplayName("createActionGuide 메소드 테스트")
    class CreateActionGuideTest {

        @Test
        @DisplayName("DataAccessResourceFailureException 발생 시 DB 연결 실패 가이드를 반환해야 한다")
        void givenDataAccessResourceFailureException_whenCreateActionGuide_thenReturnsConnectionGuide() {
            // given
            Throwable exception = new DataAccessResourceFailureException("DB 연결 실패");

            // when
            String result = ErrorLogsUtils.createActionGuide(exception);

            // then
            assertTrue(result.contains("[데이터베이스 연결 실패 오류에 대한 해결방안 입니다.]"));
        }

        @Test
        @DisplayName("InvalidDataAccessResourceUsageException 발생 시 SQL 문법 오류 가이드를 반환해야 한다")
        void givenInvalidDataAccessResourceUsageException_whenCreateActionGuide_thenReturnsSqlGuide() {
            // given
            Throwable exception = new InvalidDataAccessResourceUsageException("잘못된 SQL");

            // when
            String result = ErrorLogsUtils.createActionGuide(exception);

            // then
            assertTrue(result.contains("[컬럼, 테이블 오류 등 잘못된 DB 리소스 사용에 대한 해결방안 입니다.]"));
        }

        @Test
        @DisplayName("DataIntegrityViolationException 발생 시 무결성 제약조건 위반 가이드를 반환해야 한다")
        void givenDataIntegrityViolationException_whenCreateActionGuide_thenReturnsIntegrityGuide() {
            // given
            Throwable exception = new DataIntegrityViolationException("무결성 위반");

            // when
            String result = ErrorLogsUtils.createActionGuide(exception);

            // then
            assertTrue(result.contains("[무결성 제약조건 위반에 대한 해결방안입니다.]"));
        }

        @Test
        @DisplayName("정의되지 않은 예외 발생 시 기본 메시지를 반환해야 한다")
        void givenUndefinedException_whenCreateActionGuide_thenReturnsDefaultGuide() {
            // given
            Throwable exception = new NullPointerException("정의되지 않은 예외");

            // when
            String result = ErrorLogsUtils.createActionGuide(exception);

            // then
            assertTrue(result.contains("[오류가 발생하였습니다.]"));
        }

        @Test
        @DisplayName("null 입력 시 특정 문자열을 반환해야 한다")
        void givenNull_whenCreateActionGuide_thenReturnsSpecificString() {
            // when
            String result = ErrorLogsUtils.createActionGuide(null);

            // then
            assertEquals("throwable 값이 없습니다.", result);
        }
    }
}