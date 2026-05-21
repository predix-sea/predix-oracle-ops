package com.predix.oracle.service;

import com.predix.oracle.client.dto.MarketDto;
import com.predix.oracle.domain.ResolutionPhase;
import com.predix.oracle.exception.OracleException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResolutionStateMachineTest {

    private final ResolutionStateMachine machine = new ResolutionStateMachine();

    @Test
    void allowsEvidenceFromClosed() {
        MarketDto market = MarketDto.builder().marketId("m1").status("CLOSED").build();
        assertThatCode(() -> machine.validateTransition(market, ResolutionPhase.EVIDENCE_COLLECT))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsProposeFromClosed() {
        MarketDto market = MarketDto.builder().marketId("m1").status("CLOSED").build();
        assertThatThrownBy(() -> machine.validateTransition(market, ResolutionPhase.PROPOSE))
                .isInstanceOf(OracleException.class);
    }

    @Test
    void allowsSettleFromResolving() {
        MarketDto market = MarketDto.builder().marketId("m1").status("RESOLVING").build();
        assertThatCode(() -> machine.validateTransition(market, ResolutionPhase.SETTLE))
                .doesNotThrowAnyException();
    }

    @Test
    void mapsPhaseToJobType() {
        assertThat(machine.nextJobType(ResolutionPhase.REQUEST)).isEqualTo(com.predix.oracle.domain.JobType.REQUEST);
        assertThat(machine.nextJobType(ResolutionPhase.SETTLE)).isEqualTo(com.predix.oracle.domain.JobType.SETTLE);
    }
}
