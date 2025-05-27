package com.project200.undabang.common.web.advice;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.common.web.response.CommonResponse;
import jakarta.persistence.PersistenceException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리기 클래스.
 * 애플리케이션 전체에서 발생하는 다양한 예외를 중앙에서 처리하여 일관된 응답 형식을 제공합니다.
 *
 * <p>이 클래스는 {@link RestControllerAdvice} 어노테이션을 사용하여 모든 컨트롤러에서 발생하는
 * 예외를 감지하고 처리합니다. 각 예외 유형에 따라 적절한 HTTP 상태 코드와 응답 본문을 생성합니다.</p>
 *
 * @author Project200 Team
 * @see RestControllerAdvice
 * @see ExceptionHandler
 * @see CustomException
 * @see CommonResponse
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 로직에서 발생하는 커스텀 예외를 처리합니다.
     *
     * <p>이 메서드는 {@link CustomException} 타입의 예외를 처리하며,
     * 예외에 포함된 {@link ErrorCode}를 사용하여 적절한 HTTP 상태 코드와 응답 메시지를 구성합니다.
     * 사용자 정의 메시지가 있는 경우 이를 응답에 포함시킵니다.</p>
     *
     * @param ex 처리할 커스텀 비즈니스 예외
     * @return 구성된 오류 응답
     */
    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<CommonResponse<Void>> handleBusinessException(CustomException ex) {
        ErrorCode errorCode = ex.getErrorCode();

        CommonResponse<Void> response;
        if (ex.hasCustomMessage()) {
            response = CommonResponse.<Void>error(errorCode).message(ex.getCustomMessage()).build();
        } else {
            response = CommonResponse.<Void>error(errorCode).build();
        }

        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    /**
     * 요청 객체의 @Valid나 Validator 구현체 등으로 적용된 유효성 검증 실패 예외를 처리합니다.
     *
     * <p>이 메서드는 {@link MethodArgumentNotValidException} 타입의 예외를 처리하며,
     * Bean Validation API에 의해 발생하는 유효성 검사 오류를 처리합니다.
     * 유효성 검사에 실패한 필드와 오류 메시지를 수집하여 클라이언트에 상세 정보를 제공합니다.</p>
     *
     * @param ex 처리할 유효성 검증 예외
     * @return 필드별 오류 메시지를 포함한 응답
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<CommonResponse<Map<String, String>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex) {

        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;

        // 필드별 오류 메시지 수집
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        // 전체 오류 메시지 구성
        String customMessage = "입력값 검증에 실패했습니다.";

        // 상세 오류 정보를 data 필드에 포함
        CommonResponse<Map<String, String>> response = CommonResponse.<Map<String, String>>error(errorCode)
                .message(customMessage).data(errors).build();

        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    /**
     * 컨트롤러 메서드의 파라미터 검증 실패 예외를 처리합니다.
     *
     * <p>이 메서드는 {@link HandlerMethodValidationException} 타입의 예외를 처리하며,
     * Spring 6.1부터 도입된 파라미터 레벨 검증 실패 시 발생하는 예외를 처리합니다.
     * 유효성 검사에 실패한 파라미터와 오류 메시지를 수집하여 클라이언트에 상세 정보를 제공합니다.</p>
     *
     * @param ex 처리할 파라미터 유효성 검증 예외
     * @return 파라미터별 오류 메시지를 포함한 응답
     */

    @ExceptionHandler(HandlerMethodValidationException.class)
    protected ResponseEntity<CommonResponse<Map<String, String>>> handleMethodValidationException(
            HandlerMethodValidationException ex) {

        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;

        // 필드별 오류 메시지 수집
        Map<String, String> errors = new HashMap<>();
        ex.getParameterValidationResults().forEach(result -> {
            String paramName = result.getMethodParameter().getParameterName();
            result.getResolvableErrors().forEach(error ->
                    errors.put(paramName != null ? paramName : "param", error.getDefaultMessage())
            );
        });

        // 전체 오류 메시지 구성
        String customMessage = "입력값 검증중 실패했습니다.";

        // 상세 오류 정보를 data 필드에 포함
        CommonResponse<Map<String, String>> response = CommonResponse.<Map<String, String>>error(errorCode)
                .message(customMessage).data(errors).build();

        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }


    /**
     * ConstraintViolationException 예외를 처리합니다.
     * 이 메서드는 서비스 계층에서 @Validated 사용 시 메서드 파라미터 유효성 검증 실패로 인해 발생된
     * {@link ConstraintViolationException} 예외를 처리하며,
     * 유효성 검증 실패 항목(field, message)을 수집하여 클라이언트에 전달할 응답을 생성합니다.
     *
     * @param ex 처리할 {@link ConstraintViolationException} 예외
     * @return 유효성 검증 실패 데이터를 포함한 오류 응답
     */
    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<CommonResponse<Map<String, String>>> handleConstraintViolationException(
            ConstraintViolationException ex) {

        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;

        // 유효성 검증 실패 항목 수집
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                errors.put(violation.getPropertyPath().toString(), violation.getMessage())
        );

        String customMessage = "엔티티 유효성 검증에 실패했습니다.";

        CommonResponse<Map<String, String>> response = CommonResponse.<Map<String, String>>error(errorCode)
                .message(customMessage).data(errors).build();

        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    /**
     * 데이터베이스 저장 실패와 관련된 예외를 처리합니다.
     *
     * <p>이 메서드는 JPA 및 데이터베이스 저장 작업 중 발생할 수 있는 다양한 예외를
     * 처리하고 일관된 오류 응답을 제공합니다.</p>
     *
     * @param ex 데이터베이스 저장 관련 예외
     * @return 구성된 오류 응답
     */
    @ExceptionHandler({
            DataIntegrityViolationException.class,
            JpaSystemException.class,
            PersistenceException.class,
            OptimisticLockingFailureException.class
    })
    protected ResponseEntity<CommonResponse<Void>> handleDatabaseException(Exception ex) {
        log.error("데이터베이스 저장 실패: ", ex);
        ErrorCode errorCode = ErrorCode.MEMBER_SAVE_FAILED_ERROR;

        CommonResponse<Void> response = CommonResponse.<Void>error(errorCode).build();
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    /**
     * HttpMessageNotReadableException 예외를 처리합니다.
     *
     * <p>요청 본문을 읽을 수 없거나 파싱할 수 없을 때 발생하는 예외를 처리합니다.</p>
     *
     * @param ex 처리할 HttpMessageNotReadableException 예외
     * @return 유효하지 않은 요청에 대한 오류 응답
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    protected ResponseEntity<CommonResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("HttpMessageNotReadableException 발생: ", ex);
        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        CommonResponse<Void> response = CommonResponse.<Void>error(errorCode).message("올바른 값을 입력해 주세요").build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }


    /**
     * RequestParameter에 Long, Int가 아닌 String이 입력되었을때 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<CommonResponse<Map<String, String>>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex) {

        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;

        Map<String, String> errors = new HashMap<>();
        errors.put(ex.getName(), "올바른 형식으로 입력해주세요.");

        String customMessage = "잘못된 형식의 입력값입니다.";

        CommonResponse<Map<String, String>> response = CommonResponse.<Map<String, String>>error(errorCode)
                .message(customMessage).data(errors).build();

        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    /**
     * 다른 모든 처리되지 않은 예외를 처리합니다. (최후의 방어선)
     *
     * <p>이 메서드는 애플리케이션에서 명시적으로 처리되지 않은 모든 예외를 포착하여
     * 내부 서버 오류로 처리합니다. 예외의 자세한 내용은 로그에 기록됩니다.</p>
     *
     * <p>이 핸들러는 최후의 방어선 역할을 하여, 예기치 않은 예외가 클라이언트에게
     * 직접 노출되지 않도록 보호합니다.</p>
     *
     * @param ex 처리되지 않은 예외
     * @return 내부 서버 오류 응답
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<CommonResponse<Void>> handleException(Exception ex) {
        log.error("처리되지 않은 예외 발생: ", ex);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        CommonResponse<Void> response = CommonResponse.<Void>error(errorCode).build();
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }
}