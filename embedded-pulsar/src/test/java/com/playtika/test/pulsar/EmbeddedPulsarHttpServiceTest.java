package com.playtika.test.pulsar;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddedPulsarHttpServiceTest extends AbstractEmbeddedPulsarTest {

    private static final String METRICS_PATH = "/admin/broker-stats/metrics";

    private final TestRestTemplate testRestTemplate = new TestRestTemplate();

    @Value("${embedded.pulsar.httpServiceUrl}")
    private String pulsarServiceUrl;

    @Test
    void shouldCommunicateWithPulsarHttpService() {
        URI pulsarHttpService = URI.create(pulsarServiceUrl + METRICS_PATH);
        ResponseEntity<String> response = testRestTemplate.getForEntity(pulsarHttpService, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
