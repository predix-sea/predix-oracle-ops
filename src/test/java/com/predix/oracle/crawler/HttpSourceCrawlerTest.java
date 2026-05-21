package com.predix.oracle.crawler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.predix.oracle.exception.OracleException;
import com.predix.oracle.repository.entity.OracleSourceEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HttpSourceCrawlerTest {

    @Mock private RestTemplate restTemplate;
    @InjectMocks private HttpSourceCrawler crawler;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parsesOutcomeFromJson() throws Exception {
        crawler = new HttpSourceCrawler(restTemplate, mapper);
        OracleSourceEntity source = OracleSourceEntity.builder()
                .name("s1")
                .baseUrl("https://api.example.com/outcome")
                .build();
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"outcome\":\"YES\",\"confidence\":\"0.95\"}"));

        SourceFetchResult result = crawler.fetch(source, "m1");

        assertThat(result.getNormalizedOutcome()).isEqualTo("YES");
        assertThat(result.getConfidenceScore().toPlainString()).isEqualTo("0.95");
    }

    @Test
    void failsOnNon2xx() {
        crawler = new HttpSourceCrawler(restTemplate, mapper);
        OracleSourceEntity source = OracleSourceEntity.builder()
                .name("s1")
                .baseUrl("https://api.example.com/outcome")
                .build();
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("error", HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> crawler.fetch(source, "m1"))
                .isInstanceOf(OracleException.class);
    }
}
