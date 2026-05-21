package com.predix.oracle.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketDto {
    private String marketId;
    private String status;
    private String question;
    private List<String> outcomeCodes;
    private String winningOutcome;
    private String assertionId;
    private String requestId;
}
