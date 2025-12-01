package com.playtika.testcontainer.pulsar;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddedPulsarHttpServiceTest extends AbstractEmbeddedPulsarTest {

    private static final String METRICS_PATH = "/admin/broker-stats/metrics";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${embedded.pulsar.httpServiceUrl}")
    private String pulsarServiceUrl;

    @Test
    void shouldCommunicateWithPulsarHttpService() {
        URI pulsarHttpService = URI.create(pulsarServiceUrl + METRICS_PATH);
        ResponseEntity<String> response = restTemplate.getForEntity(pulsarHttpService, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
