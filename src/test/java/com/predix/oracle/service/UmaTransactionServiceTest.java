package com.predix.oracle.service;

import com.predix.oracle.config.PredixOracleProperties;
import com.predix.oracle.domain.UmaActionType;
import com.predix.oracle.oracle.UmaChainResult;
import com.predix.oracle.repository.UmaTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UmaTransactionServiceTest {

    @Mock private UmaTransactionRepository repository;
    @Mock private PredixOracleProperties properties;

    @InjectMocks private UmaTransactionService service;

    @Test
    void recordsTransaction() {
        PredixOracleProperties.UmaProperties uma = new PredixOracleProperties.UmaProperties();
        uma.setChainId(137L);
        when(properties.getUma()).thenReturn(uma);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var tx = service.record("m1", UmaActionType.REQUEST, UmaChainResult.builder()
                .txHash("0x1")
                .assertionId("ast")
                .requestId("req")
                .receipt(Map.of("ok", true))
                .build());

        assertThat(tx.getTxHash()).isEqualTo("0x1");
        assertThat(tx.getChainId()).isEqualTo(137L);
    }
}
