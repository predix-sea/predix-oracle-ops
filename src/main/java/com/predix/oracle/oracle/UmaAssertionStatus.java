package com.predix.oracle.oracle;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UmaAssertionStatus {
    String assertionId;
    String requestId;
    String state;
    boolean disputed;
    boolean settled;
    String winningOutcome;
}
