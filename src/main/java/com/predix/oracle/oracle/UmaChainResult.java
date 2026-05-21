package com.predix.oracle.oracle;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class UmaChainResult {
    String txHash;
    String assertionId;
    String requestId;
    Map<String, Object> receipt;
}
