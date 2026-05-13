package com.runway.common.exception;

import lombok.Getter;

@Getter
public class RunwayException extends RuntimeException {

    private final ErrorCode errorCode;

    public RunwayException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public RunwayException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }
}
