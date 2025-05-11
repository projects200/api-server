package com.project200.undabang.common.web.exception;

public class CustomException extends RuntimeException {
    private ErrorCode errorCode;
    private String customMessage;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.customMessage = null;
    }

    public CustomException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
        this.customMessage = customMessage;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getCustomMessage() {
        return customMessage;
    }

    public boolean hasCustomMessage() {
        return customMessage != null && !customMessage.equals(errorCode.getMessage());
    }

}