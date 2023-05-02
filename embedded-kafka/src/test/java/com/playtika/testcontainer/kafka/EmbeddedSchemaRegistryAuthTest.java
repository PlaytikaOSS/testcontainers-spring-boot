package com.playtika.testcontainer.kafka;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

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
        TestRestTemplate restTemplate = new TestRestTemplate(new RestTemplateBuilder()
                .rootUri(String.format("http://%s:%d", host, port))
                .basicAuthentication(username, password));

        ResponseEntity<String> response = restTemplate.getForEntity("/", String.class);

        assertThat(response)
                .extracting(ResponseEntity::getStatusCode)
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void authenticationFailed() {
        TestRestTemplate restTemplate = new TestRestTemplate(new RestTemplateBuilder()
                .rootUri(String.format("http://%s:%d", host, port)));

        ResponseEntity<String> response = restTemplate.getForEntity("/", String.class);

        assertThat(response)
                .extracting(ResponseEntity::getStatusCode)
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
