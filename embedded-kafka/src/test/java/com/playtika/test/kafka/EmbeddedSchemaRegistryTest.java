package com.playtika.test.kafka;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

@Order(4)
class EmbeddedSchemaRegistryTest extends AbstractEmbeddedKafkaTest {

    @Value("${embedded.kafka.schema-registry.host}")
    private String host;

    @Value("${embedded.kafka.schema-registry.port}")
    private Integer port;

    @Test
    void schemasTopicAvailable() throws Exception {
        assertThatTopicExists("_schemas", 1);
    }

    @Test
    void schemaCreation() {
        TestRestTemplate restTemplate = new TestRestTemplate(new RestTemplateBuilder()
                .rootUri(String.format("http://%s:%d", host, port)));

        HttpHeaders headers = new HttpHeaders();
        headers.set(CONTENT_TYPE, "application/vnd.schemaregistry.v1+json");

        HttpEntity<String> request = new HttpEntity<>("{\"schema\": \"{\\\"type\\\": \\\"string\\\"}\"}", headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/subjects/test-subject/versions", request, String.class);

        assertThat(response)
                .extracting(ResponseEntity::getStatusCode, ResponseEntity::getBody)
                .contains(HttpStatus.OK, "{\"id\":1}");
    }
}
