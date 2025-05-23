package com.project200.undabang.common.web.exception;

public class S3UploadFailedException extends RuntimeException {
    public S3UploadFailedException(String message) {
        super(message);
    }

    public S3UploadFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
