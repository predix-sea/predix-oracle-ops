package com.predix.oracle.oracle;

import com.predix.oracle.client.dto.MarketDto;
import com.predix.oracle.config.PredixOracleProperties;
import com.predix.oracle.exception.OracleErrorCode;
import com.predix.oracle.exception.OracleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "predix.oracle.uma.enabled", havingValue = "true")
public class Web3jUmaClient implements UmaClient {

    private final PredixOracleProperties properties;
    private final Web3j web3j;
    private final RawTransactionManager txManager;

    public Web3jUmaClient(PredixOracleProperties properties) {
        this.properties = properties;
        this.web3j = Web3j.build(new HttpService(properties.getUma().getRpcUrl()));
        Credentials credentials = Credentials.create(properties.getUma().getPrivateKey());
        this.txManager = new RawTransactionManager(web3j, credentials, properties.getUma().getChainId());
    }

    @Override
    public UmaChainResult requestResolution(MarketDto market) {
        return submitPlaceholder("REQUEST", market.getMarketId());
    }

    @Override
    public UmaChainResult proposeOutcome(MarketDto market, String outcome) {
        return submitPlaceholder("PROPOSE", market.getMarketId() + ":" + outcome);
    }

    @Override
    public UmaChainResult disputeOutcome(String assertionId) {
        return submitPlaceholder("DISPUTE", assertionId);
    }

    @Override
    public UmaChainResult settleResolution(String assertionId) {
        return submitPlaceholder("SETTLE", assertionId);
    }

    @Override
    public UmaAssertionStatus getAssertionStatus(String assertionId) {
        return UmaAssertionStatus.builder()
                .assertionId(assertionId)
                .state("ON_CHAIN_QUERY")
                .build();
    }

    private UmaChainResult submitPlaceholder(String action, String ref) {
        try {
            // Production: encode Optimistic Oracle V3 calls against ooContract
            String txHash = "0x" + UUID.randomUUID().toString().replace("-", "");
            log.info("UMA {} submitted (placeholder) ref={} contract={}", action, ref,
                    properties.getUma().getOoContract());
            return UmaChainResult.builder()
                    .txHash(txHash)
                    .assertionId("ast-" + ref)
                    .requestId("req-" + ref)
                    .receipt(Map.of("action", action, "gasProvider", DefaultGasProvider.GAS_LIMIT))
                    .build();
        } catch (Exception e) {
            throw new OracleException(OracleErrorCode.UMA_REQUEST_FAILED, "Chain submit failed: " + action, e);
        }
    }
}
