package com.predix.oracle.exception;

import com.predix.oracle.controller.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsOracleException() {
        ResponseEntity<ApiResponse<Void>> res = handler.handleOracle(
                new OracleException(OracleErrorCode.EVIDENCE_INSUFFICIENT, "need more"),
                null);
        assertThat(res.getStatusCode().value()).isEqualTo(400);
        assertThat(res.getBody().getCode()).isEqualTo("EVIDENCE_INSUFFICIENT");
    }
}
