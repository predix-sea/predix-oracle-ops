package com.predix.oracle.oracle;

import com.predix.oracle.client.dto.MarketDto;
import com.predix.oracle.config.PredixOracleProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StubUmaClientTest {

    @Test
    void requestProposeSettleLifecycle() {
        PredixOracleProperties props = new PredixOracleProperties();
        StubUmaClient client = new StubUmaClient(props);
        MarketDto market = MarketDto.builder().marketId("m99").build();

        UmaChainResult request = client.requestResolution(market);
        assertThat(request.getAssertionId()).contains("m99");

        client.proposeOutcome(market, "YES");
        UmaAssertionStatus proposed = client.getAssertionStatus(request.getAssertionId());
        assertThat(proposed.getState()).isEqualTo("PROPOSED");

        client.settleResolution(request.getAssertionId());
        assertThat(client.getAssertionStatus(request.getAssertionId()).isSettled()).isTrue();
    }

    @Test
    void disputeMarksAssertion() {
        StubUmaClient client = new StubUmaClient(new PredixOracleProperties());
        MarketDto market = MarketDto.builder().marketId("d1").build();
        UmaChainResult req = client.requestResolution(market);
        client.disputeOutcome(req.getAssertionId());
        assertThat(client.getAssertionStatus(req.getAssertionId()).isDisputed()).isTrue();
    }
}
