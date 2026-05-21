package com.predix.oracle.controller;

import com.predix.oracle.controller.dto.ApiResponse;
import com.predix.oracle.controller.dto.CreateSourceRequest;
import com.predix.oracle.controller.dto.PatchSourceRequest;
import com.predix.oracle.repository.entity.OracleSourceEntity;
import com.predix.oracle.service.OracleSourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/oracle/sources")
@RequiredArgsConstructor
public class OracleSourceController {

    private final OracleSourceService sourceService;

    @GetMapping
    public ApiResponse<List<OracleSourceEntity>> list() {
        return ApiResponse.ok(sourceService.listAll());
    }

    @PostMapping
    public ApiResponse<OracleSourceEntity> create(@Valid @RequestBody CreateSourceRequest request) {
        return ApiResponse.ok(sourceService.create(request));
    }

    @PatchMapping("/{id}")
    public ApiResponse<OracleSourceEntity> patch(@PathVariable Long id,
                                                 @RequestBody PatchSourceRequest request) {
        return ApiResponse.ok(sourceService.patch(id, request));
    }

    @PostMapping("/{id}/enable")
    public ApiResponse<OracleSourceEntity> enable(@PathVariable Long id) {
        return ApiResponse.ok(sourceService.setEnabled(id, true));
    }

    @PostMapping("/{id}/disable")
    public ApiResponse<OracleSourceEntity> disable(@PathVariable Long id) {
        return ApiResponse.ok(sourceService.setEnabled(id, false));
    }
}
