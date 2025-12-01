package com.playtika.testcontainer.grafana;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddedGrafanaBootstrapConfigurationTest extends BaseEmbeddedGrafanaTest {
    @Value("${embedded.grafana.username}")
    private String username;
    @Value("${embedded.grafana.password}")
    private String password;

    @Test
    void shouldProvisionDatasourceFromConfigurationFile() {
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://" + grafanaHost + ":" + grafanaPort + "/api/datasources/name/Prometheus";

        HttpHeaders headers = new HttpHeaders();
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + new String(encodedAuth, StandardCharsets.UTF_8);
        headers.set("Authorization", authHeader);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(JsonPath.parse(response.getBody()).read("$.url", String.class)).isEqualTo("http://prometheus:9090");
    }

}
