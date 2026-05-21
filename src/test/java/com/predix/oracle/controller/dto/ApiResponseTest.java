package com.predix.oracle.controller.dto;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void okIncludesTraceId() {
        MDC.put("traceId", "trace-123");
        ApiResponse<String> res = ApiResponse.ok("data");
        assertThat(res.getCode()).isEqualTo("OK");
        assertThat(res.getData()).isEqualTo("data");
        assertThat(res.getTraceId()).isEqualTo("trace-123");
        MDC.clear();
    }
}
