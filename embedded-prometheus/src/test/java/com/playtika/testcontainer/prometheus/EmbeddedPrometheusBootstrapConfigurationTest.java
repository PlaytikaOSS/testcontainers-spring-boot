package com.playtika.testcontainer.prometheus;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddedPrometheusBootstrapConfigurationTest extends BaseEmbeddedPrometheusTest {

    @Test
    void shouldHaveMetrics() {
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://" + prometheusHost + ":" + prometheusPort + "/api/v1/query?query=up";

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(JsonPath.parse(response.getBody()).read("$.status", String.class)).isEqualTo("success");
    }
}