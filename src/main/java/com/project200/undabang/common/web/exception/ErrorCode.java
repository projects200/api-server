package com.project200.undabang.common.web.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public enum ErrorCode {
    // 공통 에러
    INVALID_INPUT_VALUE(400, "INVALID_INPUT_VALUE", "유효하지 않은 입력 값입니다."),
    METHOD_NOT_ALLOWED(405, "METHOD_NOT_ALLOWED", "지원하지 않는 HTTP 메소드입니다."),
    INTERNAL_SERVER_ERROR(500, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),

    // 인증 관련 에러
    AUTHENTICATION_FAILED(401, "AUTHENTICATION_FAILED", "인증에 실패했습니다."),
    AUTHORIZATION_DENIED(403, "AUTHORIZATION_DENIED", "접근 권한이 없습니다."),
    USER_ID_HEADER_MISSING(401, "USER_ID_HEADER_MISSING", "X-USER-ID 헤더가 누락되었습니다."),
    USER_EMAIL_HEADER_MISSING(401, "USER_EMAIL_HEADER_MISSING", "X-USER-EMAIL 헤더가 누락되었습니다."),
    INVALID_USER_ID_FORMAT(400, "INVALID_USER_ID_FORMAT", "X-USER-ID 헤더는 유효한 UUID 형식이어야 합니다."),

    // 사용자 관련 에러
    MEMBER_NOT_FOUND(404, "USER_NOT_FOUND", "해당 사용자를 찾을 수 없습니다."),
    MEMBER_EMAIL_DUPLICATED(409, "MEMBER_EMAIL_DUPLICATED", "이미 사용 중인 이메일입니다."),
    MEMBER_ID_DUPLICATED(409, "MEMBER_ID_DUPLICATED", "이미 가입한 회원 입니다."),
    MEMBER_NICKNAME_DUPLICATED(409, "MEMBER_NICKNAME_DUPLICATED", "이미 사용 중인 닉네임입니다."),
    MEMBER_GENDER_ERROR(409, "MEMBER_GENDER_ERROR", "유효하지 않은 입력 값입니다."),
    MEMBER_BDAY_ERROR(409, "MEMBER_BDAY_ERROR", "유효하지 않은 입력 값입니다."),
    MEMBER_SAVE_FAILED_ERROR(500, "MEMBER_SAVE_FAILED_ERROR", "모종의 에러로 회원가입에 실패하였습니다."),

    // 운동기록 관련 에러
    EXERCISE_NOT_FOUND(404, "EXERCISE_NOT_FOUND", "운동 기록을 찾을 수 없습니다."),
    IMPOSSIBLE_INPUT_DATE(400, "IMPOSSIBLE_INPUT_DATE", "검색 기간을 올바로 설정하세요."),
    EXERCISE_RECORD_NOT_FOUND(404, "EXERCISE_RECORD_NOT_FOUND", "운동 기록을 찾을 수 없습니다."),
    EXERCISE_PICTURE_UPLOAD_FAILED(500, "EXERCISE_PICTURE_UPLOAD_FAILED", "운동 이미지 업로드에 실패했습니다."),
    EXERCISE_PICTURE_DELETE_FAILED(500, "EXERCISE_PICTURE_DELETE_FAILED", "운동 이미지 수정에 실패했습니다.");

    private final HttpStatusCode status;
    private final String code;
    private final String message;

    ErrorCode(int status, String code, String message) {
        this.status = HttpStatus.valueOf(status);
        this.code = code;
        this.message = message;
    }

    public HttpStatusCode getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
