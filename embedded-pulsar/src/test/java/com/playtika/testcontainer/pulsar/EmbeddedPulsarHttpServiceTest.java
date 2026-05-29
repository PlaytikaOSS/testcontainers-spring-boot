package com.playtika.testcontainer.pulsar;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddedPulsarHttpServiceTest extends AbstractEmbeddedPulsarTest {

    private static final List<String> METRICS_PATHS = List.of(
            "/metrics",
            "/admin/v2/broker-stats/metrics",
            "/admin/broker-stats/metrics"
    );

    private final TestRestTemplate testRestTemplate = new TestRestTemplate();

    @Value("${embedded.pulsar.httpServiceUrl}")
    private String pulsarServiceUrl;

    @Test
    void shouldCommunicateWithPulsarHttpService() {
        List<HttpStatusCode> statuses = METRICS_PATHS.stream()
                .map(path -> URI.create(pulsarServiceUrl + path))
                .map(uri -> testRestTemplate.getForEntity(uri, String.class).getStatusCode())
                .toList();

        assertThat(statuses)
                .withFailMessage("Expected at least one Pulsar metrics endpoint to return 200 OK, but got statuses %s", statuses)
                .contains(HttpStatus.OK);
    }
}
