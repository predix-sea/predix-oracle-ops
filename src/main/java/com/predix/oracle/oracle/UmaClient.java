package com.predix.oracle.oracle;

import com.predix.oracle.client.dto.MarketDto;

public interface UmaClient {

    UmaChainResult requestResolution(MarketDto market);

    UmaChainResult proposeOutcome(MarketDto market, String outcome);

    UmaChainResult disputeOutcome(String assertionId);

    UmaChainResult settleResolution(String assertionId);

    UmaAssertionStatus getAssertionStatus(String assertionId);
}
