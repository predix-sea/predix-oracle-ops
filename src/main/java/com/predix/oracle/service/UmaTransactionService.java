package com.predix.oracle.service;

import com.predix.oracle.domain.TxStatus;
import com.predix.oracle.domain.UmaActionType;
import com.predix.oracle.oracle.UmaChainResult;
import com.predix.oracle.repository.UmaTransactionRepository;
import com.predix.oracle.repository.entity.UmaTransactionEntity;
import com.predix.oracle.config.PredixOracleProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UmaTransactionService {

    private final UmaTransactionRepository repository;
    private final PredixOracleProperties properties;

    @Transactional
    public UmaTransactionEntity record(String marketId, UmaActionType action, UmaChainResult result) {
        UmaTransactionEntity tx = UmaTransactionEntity.builder()
                .marketId(marketId)
                .actionType(action)
                .chainId(properties.getUma().getChainId())
                .txHash(result.getTxHash())
                .assertionId(result.getAssertionId())
                .requestId(result.getRequestId())
                .txStatus(TxStatus.CONFIRMED)
                .submittedAt(Instant.now())
                .confirmedAt(Instant.now())
                .rawReceipt(result.getReceipt())
                .build();
        return repository.save(tx);
    }

    public List<UmaTransactionEntity> listByMarket(String marketId) {
        return repository.findByMarketIdOrderByCreatedAtDesc(marketId);
    }
}
