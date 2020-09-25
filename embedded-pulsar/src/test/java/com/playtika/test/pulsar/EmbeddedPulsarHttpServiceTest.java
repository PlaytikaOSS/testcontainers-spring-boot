package com.playtika.test.pulsar;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

public class EmbeddedPulsarHttpServiceTest extends AbstractEmbeddedPulsarTest {

    private static final String METRICS_PATH = "/admin/broker-stats/metrics";

    private final TestRestTemplate testRestTemplate = new TestRestTemplate();

    @Autowired
    private ConfigurableEnvironment environment;

    @Test
    void shouldCommunicateWithPulsarHttpService() {
        //given
        String pulsarServiceUrl = environment.getProperty("embedded.pulsar.httpServiceUrl");
        URI pulsarHttpService = URI.create(pulsarServiceUrl + METRICS_PATH);

        //when
        ResponseEntity<String> response = testRestTemplate.getForEntity(pulsarHttpService, String.class);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    }
}
