package com.playtika.testcontainer.kafka;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Order(5)
@TestPropertySource(properties = {
        "embedded.kafka.schema-registry.enabled=true",
        "embedded.kafka.schema-registry.authentication=BASIC"
})
class EmbeddedSchemaRegistryAuthTest extends AbstractEmbeddedKafkaTest {

    @Value("${embedded.kafka.schema-registry.host}")
    private String host;

    @Value("${embedded.kafka.schema-registry.port}")
    private Integer port;

    @Value("${embedded.kafka.schema-registry.username}")
    private String username;

    @Value("${embedded.kafka.schema-registry.password}")
    private String password;

    @Test
    void authenticationSucceeded() {
        RestTemplate restTemplate = new RestTemplate();
        String baseUrl = String.format("http://%s:%d", host, port);

        HttpHeaders headers = new HttpHeaders();
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + new String(encodedAuth, StandardCharsets.UTF_8);
        headers.set("Authorization", authHeader);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/",
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response)
                .extracting(ResponseEntity::getStatusCode)
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void authenticationFailed() {
        RestTemplate restTemplate = new RestTemplate();
        String baseUrl = String.format("http://%s:%d", host, port);

        assertThatThrownBy(() -> restTemplate.getForEntity(baseUrl + "/", String.class))
                .isInstanceOf(HttpClientErrorException.class)
                .extracting(ex -> ((HttpClientErrorException) ex).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
