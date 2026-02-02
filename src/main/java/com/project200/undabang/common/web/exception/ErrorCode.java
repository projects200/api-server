package com.project200.undabang.common.web.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public enum ErrorCode {
    // 공통 에러
    INVALID_INPUT_VALUE(400, "INVALID_INPUT_VALUE", "유효하지 않은 입력 값입니다."),
    METHOD_NOT_ALLOWED(405, "METHOD_NOT_ALLOWED", "지원하지 않는 HTTP 메소드입니다."),
    INTERNAL_SERVER_ERROR(500, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),
    SERVICE_UNAVAILABLE(503, "SERVICE_UNAVAILABLE", "일시적으로 서버를 이용할 수 없습니다."),

    // 이미지 에러
    INVALID_FILE_NAME(400, "INVALID_FILE_NAME", "파일 이름이 유효하지 않습니다."),

    // 인증 관련 에러
    AUTHENTICATION_FAILED(401, "AUTHENTICATION_FAILED", "인증에 실패했습니다."),
    AUTHORIZATION_DENIED(403, "AUTHORIZATION_DENIED", "접근 권한이 없습니다."),
    USER_ID_HEADER_MISSING(401, "USER_ID_HEADER_MISSING", "X-USER-ID 헤더가 누락되었습니다."),
    USER_EMAIL_HEADER_MISSING(401, "USER_EMAIL_HEADER_MISSING", "X-USER-EMAIL 헤더가 누락되었습니다."),
    INVALID_USER_ID_FORMAT(400, "INVALID_USER_ID_FORMAT", "X-USER-ID 헤더는 유효한 UUID 형식이어야 합니다."),
    FCM_DEVICE_INFO_REQUIRED(400, "FCM_DEVICE_INFO_REQUIRED", "FCM 기기 정보가 필요합니다"),
    LOGIN_FAILED(401, "LOGIN_FAILED", "로그인에 실패했습니다. 일치하는 회원을 찾을 수 없습니다."),
    LOGOUT_FAILED(401, "LOGOUT_FAILED", "로그아웃에 실패했습니다. 일치하는 회원을 찾을 수 없습니다."),

    // 사용자 관련 에러
    MEMBER_SELF_REQUEST_NOT_ALLOWED(400, "MEMBER_SELF_REQUEST_NOT_ALLOWED", "자기 자신을 대상으로 한 요청은 유효하지 않습니다"),
    MEMBER_NOT_FOUND(404, "USER_NOT_FOUND", "해당 사용자를 찾을 수 없습니다."),
    MEMBER_EMAIL_DUPLICATED(409, "MEMBER_EMAIL_DUPLICATED", "이미 사용 중인 이메일입니다."),
    MEMBER_ID_DUPLICATED(409, "MEMBER_ID_DUPLICATED", "이미 가입한 회원 입니다."),
    MEMBER_NICKNAME_DUPLICATED(409, "MEMBER_NICKNAME_DUPLICATED", "이미 사용 중인 닉네임입니다."),
    MEMBER_GENDER_ERROR(400, "MEMBER_GENDER_ERROR", "유효하지 않은 성별 값입니다."),
    MEMBER_BDAY_ERROR(400, "MEMBER_BDAY_ERROR", "유효하지 않은 생년월일 값입니다."),
    MEMBER_SAVE_FAILED_ERROR(500, "MEMBER_SAVE_FAILED_ERROR", "모종의 에러로 회원가입에 실패하였습니다."),

    // 운동기록 관련 에러
    EXERCISE_NOT_FOUND(404, "EXERCISE_NOT_FOUND", "운동 기록을 찾을 수 없습니다."),
    IMPOSSIBLE_INPUT_DATE(400, "IMPOSSIBLE_INPUT_DATE", "검색 기간을 올바로 설정하세요."),
    EXERCISE_RECORD_NOT_FOUND(404, "EXERCISE_RECORD_NOT_FOUND", "운동 기록을 찾을 수 없습니다."),
    EXERCISE_PICTURE_UPLOAD_FAILED(500, "EXERCISE_PICTURE_UPLOAD_FAILED", "운동 이미지 업로드에 실패했습니다."),
    EXERCISE_PICTURE_DELETE_FAILED(500, "EXERCISE_PICTURE_DELETE_FAILED", "운동 이미지 수정에 실패했습니다."),
    EXERCISE_PICTURE_COUNT_EXCEEDED(400, "EXERCISE_PICTURE_COUNT_EXCEEDED", "운동 사진은 최대 5개까지만 업로드할 수 있습니다."),

    // 정책 관련 에러
    POLICY_NOT_EXIST(404, "POLICY_NOT_EXIST", "존재하지 않는 정책명 입니다."),
    POLICY_NOT_FOUND(500, "POLICY_NOT_FOUND", "정책을 찾을 수 없습니다."),

    // 심플 타이머 관련 에러
    SIMPLE_TIMER_NOT_FOUND(404, "SIMPLE_TIMER_NOT_FOUND", "존재하지 않는 타이머 입니다."),
    SIMPLE_TIMER_MAX_COUNT_VIOLATION(409, "SIMPLE_TIMER_MAX_COUNT_VIOLATION", "최대 심플 타이머 개수(6개)를 초과했습니다."),

    // 커스텀 타이머 관련 에러
    CUSTOM_TIMER_NOT_FOUND(404, "CUSTOM_TIMER_NOT_FOUND", "존재하지 않는 타이머 입니다."),
    CUSTOM_TIMER_STEP_MIN_COUNT_VIOLATION(409, "CUSTOM_TIMER_MIN_COUNT_VIOLATION", "최소 1개 이상의 커스텀 타이머 스텝을 보유해야 합니다."),
    CUSTOM_TIMER_STEP_MAX_COUNT_VIOLATION(409, "CUSTOM_TIMER_STEP_MAX_COUNT_VIOLATION", "최대 커스텀 타이머 스텝 개수(50개)를 초과했습니다."),
    CUSTOM_TIMER_STEP_ORDER_INVALID(409, "CUSTOM_TIMER_STEP_ORDER_INVALID", "스텝 순서가 잘못되었습니다."),

    // 사진 관련 에러
    PICTURE_UPLOAD_FAILED(500, "PICTURE_UPLOAD_FAILED", "이미지 업로드에 실패했습니다."),
    PICTURE_DELETE_FAILED(500, "PICTURE_DELETE_FAILED", "이미지 삭제에 실패했습니다."),
    PICTURE_IS_EMPTY(400, "PICTURE_IS_EMPTY", "요청받은 이미지가 비어있습니다."),
    PICTURE_NOT_FOUND(404, "PICTURE_NOT_FOUND", "존재하지 않는 사진입니다."),

    // 운동 주소 관련 에러
    EXERCISE_LOCATION_MAX_COUNT_VIOLATION(409, "EXERCISE_LOCATION_MAX_COUNT_VIOLATION", "최대 운동 장소 저장 갯수(10개)를 초과했습니다."),
    EXERCISE_LOCATION_NAME_DUPLICATED(409, "EXERCISE_LOCATION_NAME_DUPLICATED", "이미 사용중인 운동 장소 명 입니다."),
    EXERCISE_LOCATION_NOT_FOUND(404, "EXERCISE_LOCATION_NOT_FOUND", "존재하지 않는 운동장소 입니다"),

    // 오픈 채팅 관련 에러
    OPEN_CHAT_ROOM_ALREADY_EXIST(409, "OPEN_CHAT_ROOM_ALREADY_EXIST", "이미 오픈 채팅방을 보유한 회원입니다."),
    OPEN_CHAT_ROOM_URL_DUPLICATED(409, "OPEN_CHAT_ROOM_URL_DUPLICATED", "이미 사용중인 오픈 채팅 링크입니다"),
    OPEN_CHAT_ROOM_NOT_FOUND(404, "OPEN_CHAT_ROOM_NOT_FOUND", "존재하지 않는 오픈 채팅방 입니다."),

    // 채팅방 관련 에러
    SELF_CHAT_NOT_ALLOWED(400, "SELF_CHAT_NOT_ALLOWED", "자기 자신과 채팅방을 개설할 수 없습니다."),
    CHATROOM_CREATE_BLOCKED(403, "CHATROOM_CREATE_BLOCKED", "해당 회원님과는 채팅방을 생성할 수 없습니다."),
    MESSAGE_SEND_BLOCKED(403, "MESSAGE_SEND_BLOCKED", "해당 회원님과는 메시지를 주고받을 수 없습니다."),
    CHATROOM_MEMBERS_NOT_FOUND(404, "CHATROOM_MEMBERS_NOT_FOUND", "등록된 채팅방 참여자 정보를 확인할 수 없습니다."),
    CHATROOM_NOT_FOUND(404, "CHATROOM_NOT_FOUND", "존재하지 않는 채팅방 입니다."),
    CHAT_NOT_FOUND(404, "CHAT_NOT_FOUND", "존재하지 않는 채팅 입니다."),
    CHATROOM_MEMBER_INACTIVE(409, "CHATROOM_MEMBER_INACTIVE", "회원님이 나간 채팅방 입니다."),
    CHATROOM_OTHER_MEMBER_INACTIVE(409, "CHATROOM_OTHER_MEMBER_INACTIVE", "다른 회원님이 나간 채팅방입니다."),
    CHATROOM_CREATE_TOO_FAR_DISTANCE(409, "CHATROOM_CREATE_TOO_FAR_DISTANCE", "해당 회원님과는 채팅방을 생성하기에 너무 멀리 있습니다."),

    // 차단 관련 에러
    MEMBER_BLOCK_NOT_FOUND(404, "MEMBER_BLOCK_NOT_FOUND", "해당 회원을 차단한 이력이 없습니다."),
    MEMBER_BLOCK_DUPLICATED(409, "MEMBER_BLOCK_DUPLICATED", "이미 차단한 회원입니다."),

    // FCM 토큰 관련 에러
    FCM_TOKEN_NOT_FOUND(404, "FCM_TOKEN_NOT_FOUND", "존재하지 않는 FCM 토큰입니다."),

    // 기기 푸시 알림 관련 에러
    NOTIFICATION_TYPE_NOT_FOUND(404, "NOTIFICATION_TYPE_NOT_FOUND", "존재하지 않는 알림 타입입니다."),

    // 선호 운동 관련 에러
    PREFERRED_EXERCISE_MAX_COUNT_VIOLATION(409, "PREFERRED_EXERCISE_MAX_COUNT_VIOLATION", "선호 운동은 최대 5개까지만 등록할 수 있습니다."),
    PREFERRED_EXERCISE_DUPLICATED(409, "PREFERRED_EXERCISE_DUPLICATED", "이미 등록된 선호 운동 종목입니다."),
    PREFERRED_EXERCISE_NOT_FOUND(404, "PREFERRED_EXERCISE_NOT_FOUND", "존재하지 않는 운동 종류입니다."),
    PREFERRED_EXERCISE_DUPLICATED_IN_REQUEST(400, "PREFERRED_EXERCISE_DUPLICATED_IN_REQUEST", "요청 내 선호 운동 종목이 중복되었습니다."),

    // 피드 관련 에러
    FEED_NOT_FOUND(404, "FEED_NOT_FOUND", "존재하지 않는 피드입니다."),

    // 피드 타입 관련 에러
    FEED_TYPE_NOT_FOUND(404, "FEED_TYPE_NOT_FOUND", "존재하지 않는 피드 타입입니다."),

    // 댓글 관련 에러
    COMMENT_NOT_FOUND(404, "COMMENT_NOT_FOUND", "존재하지 않는 댓글입니다."),
    COMMENT_DELETE_FORBIDDEN(403, "COMMENT_DELETE_FORBIDDEN", "댓글 삭제 권한이 없습니다."),
    COMMENT_PARENT_NOT_FOUND(404, "COMMENT_PARENT_NOT_FOUND", "부모 댓글을 찾을 수 없습니다.");

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
