package com.predix.oracle;

import com.predix.oracle.client.MarketSchemaClient;
import com.predix.oracle.client.dto.MarketDto;
import com.predix.oracle.controller.dto.CreateSourceRequest;
import com.predix.oracle.crawler.EvidenceCollectionService;
import com.predix.oracle.crawler.HttpSourceCrawler;
import com.predix.oracle.crawler.SourceFetchResult;
import com.predix.oracle.domain.ActorType;
import com.predix.oracle.domain.JobType;
import com.predix.oracle.mq.OracleJobMessage;
import com.predix.oracle.oracle.StubUmaClient;
import com.predix.oracle.oracle.UmaClient;
import com.predix.oracle.repository.ResolutionJobRepository;
import com.predix.oracle.repository.entity.OracleSourceEntity;
import com.predix.oracle.repository.entity.ResolutionJobEntity;
import com.predix.oracle.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.redis.testcontainers.RedisContainer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@EnabledIf("com.predix.oracle.support.DockerSupport#isAvailable")
class ResolutionFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("predix_oracle")
            .withUsername("predix")
            .withPassword("predix");

    @Container
    static RedisContainer redis = new RedisContainer("redis:7-alpine");

    @Container
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management-alpine")
            .withUser("predix", "predix");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", () -> rabbit.getAmqpPort().toString());
        registry.add("spring.rabbitmq.username", () -> "predix");
        registry.add("spring.rabbitmq.password", () -> "predix");
    }

    @Autowired
    private OracleSourceService sourceService;
    @Autowired
    private EvidenceCollectionService evidenceService;
    @Autowired
    private ResolutionJobService jobService;
    @Autowired
    private ResolutionJobExecutor jobExecutor;
    @Autowired
    private ResolutionJobRepository jobRepository;
    @Autowired
    private MarketSchemaClient marketSchemaClient;
    @Autowired
    private UmaClient umaClient;

    @MockBean
    private HttpSourceCrawler httpSourceCrawler;

    @MockBean
    private com.predix.oracle.mq.OracleMqPublisher mqPublisher;

    @BeforeEach
    void seed() {
        marketSchemaClient.seedMarket(MarketDto.builder()
                .marketId("market-flow-1")
                .status("CLOSED")
                .outcomeCodes(List.of("YES", "NO"))
                .build());

        CreateSourceRequest a = new CreateSourceRequest();
        a.setName("source-a");
        a.setBaseUrl("http://localhost/a");
        sourceService.create(a);

        CreateSourceRequest b = new CreateSourceRequest();
        b.setName("source-b");
        b.setBaseUrl("http://localhost/b");
        sourceService.create(b);

        when(httpSourceCrawler.fetch(any(OracleSourceEntity.class), any()))
                .thenReturn(SourceFetchResult.builder()
                        .sourceUrl("http://mock")
                        .rawPayload(Map.of("outcome", "YES"))
                        .normalizedOutcome("YES")
                        .build());
    }

    @Test
    void fullResolutionFlowClosedToResolved() {
        evidenceService.collectForMarket("market-flow-1", ActorType.SYSTEM);

        ResolutionJobEntity requestJob = jobService.createJob(
                "market-flow-1", JobType.REQUEST,
                ResolutionJobExecutor.idempotency("market-flow-1", JobType.REQUEST),
                Map.of(), ActorType.SYSTEM);

        jobExecutor.execute(toMessage(requestJob));

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(jobRepository.findByMarketId("market-flow-1").stream()
                    .anyMatch(j -> j.getJobType() == JobType.PROPOSE)).isTrue();
        });

        ResolutionJobEntity proposeJob = jobRepository.findByMarketId("market-flow-1").stream()
                .filter(j -> j.getJobType() == JobType.PROPOSE)
                .findFirst().orElseThrow();

        jobExecutor.execute(toMessage(proposeJob));

        ResolutionJobEntity settleJob = jobService.createJob(
                "market-flow-1", JobType.SETTLE,
                ResolutionJobExecutor.idempotency("market-flow-1", JobType.SETTLE),
                Map.of("assertionId", "ast-market-flow-1"), ActorType.MANUAL);

        jobExecutor.execute(toMessage(settleJob));

        MarketDto market = marketSchemaClient.getMarket("market-flow-1");
        assertThat(market.getStatus()).isEqualTo("RESOLVED");
        assertThat(market.getWinningOutcome()).isNotBlank();
    }

    @Test
    void disputeBranchUpdatesAssertion() {
        if (umaClient instanceof StubUmaClient stub) {
            stub.requestResolution(marketSchemaClient.getMarket("market-flow-1"));
            stub.proposeOutcome(marketSchemaClient.getMarket("market-flow-1"), "YES");
            var disputed = stub.disputeOutcome("ast-market-flow-1");
            assertThat(disputed.getTxHash()).startsWith("0x");
            var status = stub.getAssertionStatus("ast-market-flow-1");
            assertThat(status.isDisputed()).isTrue();
        }
    }

    @Test
    void failedJobRetryAndDlq() {
        ResolutionJobEntity job = jobService.createJob(
                "market-flow-1", JobType.PROPOSE,
                "retry-test-key",
                Map.of("outcome", "YES"), ActorType.SYSTEM);

        jobService.markFailed(job.getId(), true);
        ResolutionJobEntity failed = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(failed.getStatus().name()).isIn("RETRYING", "FAILED");

        for (int i = 0; i < 5; i++) {
            jobService.markFailed(job.getId(), true);
        }
        ResolutionJobEntity dead = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(dead.getStatus().name()).isEqualTo("DEAD");
    }

    private OracleJobMessage toMessage(ResolutionJobEntity job) {
        return OracleJobMessage.builder()
                .jobId(job.getId())
                .marketId(job.getMarketId())
                .jobType(job.getJobType())
                .idempotencyKey(job.getIdempotencyKey())
                .actor(ActorType.SYSTEM)
                .payload(job.getPayload())
                .build();
    }
}
