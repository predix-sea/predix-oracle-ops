package com.predix.oracle.controller;

import com.predix.oracle.controller.dto.ApiResponse;
import com.predix.oracle.oracle.UmaAssertionStatus;
import com.predix.oracle.oracle.UmaClient;
import com.predix.oracle.repository.entity.UmaTransactionEntity;
import com.predix.oracle.service.UmaTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/oracle/uma")
@RequiredArgsConstructor
public class UmaController {

    private final UmaTransactionService umaTransactionService;
    private final UmaClient umaClient;

    @GetMapping("/txs")
    public ApiResponse<List<UmaTransactionEntity>> listTxs(@RequestParam String marketId) {
        return ApiResponse.ok(umaTransactionService.listByMarket(marketId));
    }

    @GetMapping("/assertions/{assertionId}")
    public ApiResponse<UmaAssertionStatus> getAssertion(@PathVariable String assertionId) {
        return ApiResponse.ok(umaClient.getAssertionStatus(assertionId));
    }
}
