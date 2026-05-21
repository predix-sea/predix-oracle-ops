package com.predix.oracle.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Data
@Configuration
@ConfigurationProperties(prefix = "predix.oracle")
public class PredixOracleProperties {

    private String authToken = "change-me";
    private EvidenceProperties evidence = new EvidenceProperties();
    private SchedulerProperties scheduler = new SchedulerProperties();
    private RetryProperties retry = new RetryProperties();
    private UmaProperties uma = new UmaProperties();
    private ClientsProperties clients = new ClientsProperties();

    @Data
    public static class EvidenceProperties {
        private int minSources = 2;
        private BigDecimal quorumRatio = new BigDecimal("0.51");
    }

    @Data
    public static class SchedulerProperties {
        private String marketScanCron = "0 */2 * * * *";
        private String assertionSyncCron = "0 */1 * * * *";
        private String retryCron = "0 */30 * * * * *";
    }

    @Data
    public static class RetryProperties {
        private int maxAttempts = 5;
        private long baseDelayMs = 1000L;
        private double multiplier = 2.0;
    }

    @Data
    public static class UmaProperties {
        private boolean enabled;
        private long chainId = 137L;
        private String rpcUrl;
        private String privateKey;
        private String ooContract;
    }

    @Data
    public static class ClientsProperties {
        private ClientEndpoint marketSchema = new ClientEndpoint();
        private MatchingEngineEndpoint matchingEngine = new MatchingEngineEndpoint();
        private ClientEndpoint eventIndexer = new ClientEndpoint();

        @Data
        public static class ClientEndpoint {
            private String baseUrl;
            private boolean enabled = true;
        }

        @Data
        public static class MatchingEngineEndpoint extends ClientEndpoint {
            private boolean enabled;
        }
    }
}
