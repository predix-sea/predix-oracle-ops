package com.predix.oracle.exception;

import com.predix.oracle.controller.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OracleException.class)
    public ResponseEntity<ApiResponse<Void>> handleOracle(OracleException ex, HttpServletRequest request) {
        HttpStatus status = mapStatus(ex.getErrorCode());
        return ResponseEntity.status(status).body(ApiResponse.error(
                ex.getErrorCode().getCode(),
                ex.getMessage(),
                traceId()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ApiResponse.error(
                OracleErrorCode.VALIDATION_ERROR.getCode(), msg, traceId()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unhandled error", ex);
        return ResponseEntity.internalServerError().body(ApiResponse.error(
                OracleErrorCode.INTERNAL_ERROR.getCode(),
                OracleErrorCode.INTERNAL_ERROR.getDefaultMessage(),
                traceId()));
    }

    private HttpStatus mapStatus(OracleErrorCode code) {
        return switch (code) {
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case VALIDATION_ERROR, EVIDENCE_INSUFFICIENT -> HttpStatus.BAD_REQUEST;
            case JOB_IDEMPOTENCY_CONFLICT, RESOLUTION_STATE_CONFLICT -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
    }

    private String traceId() {
        String id = MDC.get("traceId");
        return id != null ? id : "unknown";
    }
}
