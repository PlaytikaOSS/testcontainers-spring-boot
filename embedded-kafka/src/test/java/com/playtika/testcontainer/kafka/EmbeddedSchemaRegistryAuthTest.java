package com.playtika.testcontainer.kafka;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

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
        RestClient restClient = RestClient.builder().baseUrl(String.format("http://%s:%d", host, port)).build();
        ResponseEntity<String> response = restClient.get()
                .uri("/")
                .headers(h -> h.setBasicAuth(username, password))
                .retrieve()
                .toEntity(String.class);

        assertThat(response)
                .extracting(ResponseEntity::getStatusCode)
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void authenticationFailed() {
        RestClient restClient = RestClient.builder().baseUrl(String.format("http://%s:%d", host, port)).build();
        assertThatThrownBy(() ->{
            ResponseEntity<String> response = restClient.get().uri("/").retrieve().toEntity(String.class);
        }).isInstanceOf(HttpClientErrorException.Unauthorized.class);

    }
}
