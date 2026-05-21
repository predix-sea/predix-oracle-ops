package com.predix.oracle.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OracleErrorCode {
    SUCCESS("OK", "Success"),
    UNAUTHORIZED("UNAUTHORIZED", "Unauthorized"),
    NOT_FOUND("NOT_FOUND", "Resource not found"),
    VALIDATION_ERROR("VALIDATION_ERROR", "Validation failed"),
    ORACLE_SOURCE_UNAVAILABLE("ORACLE_SOURCE_UNAVAILABLE", "Oracle source unavailable"),
    EVIDENCE_INSUFFICIENT("EVIDENCE_INSUFFICIENT", "Insufficient evidence for resolution"),
    UMA_REQUEST_FAILED("UMA_REQUEST_FAILED", "UMA request resolution failed"),
    UMA_PROPOSE_FAILED("UMA_PROPOSE_FAILED", "UMA propose outcome failed"),
    UMA_SETTLE_FAILED("UMA_SETTLE_FAILED", "UMA settle resolution failed"),
    RESOLUTION_STATE_CONFLICT("RESOLUTION_STATE_CONFLICT", "Resolution state conflict"),
    JOB_IDEMPOTENCY_CONFLICT("JOB_IDEMPOTENCY_CONFLICT", "Job idempotency conflict"),
    INTERNAL_ERROR("INTERNAL_ERROR", "Internal server error");

    private final String code;
    private final String defaultMessage;
}
