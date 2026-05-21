package com.predix.oracle.exception;

import lombok.Getter;

@Getter
public class OracleException extends RuntimeException {

    private final OracleErrorCode errorCode;

    public OracleException(OracleErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public OracleException(OracleErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
