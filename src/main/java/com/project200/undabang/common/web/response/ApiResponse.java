package com.project200.undabang.common.web.response;

import com.project200.undabang.common.web.exception.ErrorCode;
import lombok.Getter;

/**
 * A generic class representing the standard structure of an API response.
 * It provides mechanisms to create both success and error responses with associated data.
 *
 * @param <T> The type of the data included in the response.
 */
@Getter
public class ApiResponse<T> {
    private final boolean succeed;
    private final String code;
    private final String message;
    private final T data;

    private ApiResponse(boolean succeed, String code, String message, T data) {
        this.succeed = succeed;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true,"SUCCESS", "요청이 성공적으로 처리되었습니다.", data);
    }

    public static <T> ApiResponse<T> success(String code, String message) {
        return new ApiResponse<>(true, code, message, null);
    }

    public static <T> ApiResponse<T> success(String code, String message, T data) {
        return new ApiResponse<>(true, code, message, data);
    }

    /**
     * ErrorCode로 초기화된 ErrorBuilder 객체를 반환합니다.
     * 에러 발생 상황의 경우 기본 메시지 사용, 커스텀 메시지 사용, 데이터 유무 등 많은 경우의 수가 있기에 빌드 패턴을 사용합니다.
     *
     * @param errorCode ErrorCode 열거형 값
     * @return 에러 상태로 초기화된 Builder 객체
     */
    public static <T> ErrorBuilder<T> error(ErrorCode errorCode) {
        // ErrorBuilder를 생성하며 초기값을 설정합니다.
        return new ErrorBuilder<>(errorCode);
    }

    /**
     * Error 응답을 위한 내부 빌더 클래스
     */
    public static class ErrorBuilder<T> {
        private final boolean succeed;         // 에러 상황이므로 false
        private final String code;             // ErrorCode로부터 설정
        private String message;                // ErrorCode로부터 기본 설정 후 변경 가능
        private T data;                        // 기본값은 null, 변경 가능

        // ErrorBuilder 생성자
        private ErrorBuilder(ErrorCode errorCode) {
            this.succeed = false;
            this.code = errorCode.getCode();
            this.message = errorCode.getMessage(); // 기본 메시지 설정
            this.data = null;                      // 기본 데이터는 null
        }

        /**
         * 사용자 정의 메시지로 덮어씁니다.
         *
         * @param customMessage 사용자 정의 메시지
         * @return ErrorBuilder 인스턴스 (체이닝을 위해)
         */
        public ErrorBuilder<T> message(String customMessage) {
            this.message = customMessage;
            return this; // 메소드 체이닝을 위해 자신을 반환
        }

        /**
         * 데이터를 설정합니다.
         *
         * @param data 설정할 데이터
         * @return ErrorBuilder 인스턴스 (체이닝을 위해)
         */
        public ErrorBuilder<T> data(T data) {
            this.data = data;
            return this; // 메소드 체이닝을 위해 자신을 반환
        }

        /**
         * 최종적으로 ApiResponse 객체를 생성합니다.
         *
         * @return 설정된 값으로 생성된 ApiResponse 객체
         */
        public ApiResponse<T> build() {
            return new ApiResponse<>(this.succeed, this.code, this.message, this.data);
        }
    }
}
