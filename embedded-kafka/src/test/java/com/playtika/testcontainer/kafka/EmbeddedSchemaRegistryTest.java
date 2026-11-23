package com.playtika.testcontainer.kafka;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

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
        RestClient restClient = RestClient.builder()
                .baseUrl(String.format("http://%s:%d", host, port))
                .build();
        ResponseEntity<String> response = restClient.post()
                .uri("/subjects/test-subject/versions")
                .headers(h -> h.setContentType(MediaType.parseMediaType("application/vnd.schemaregistry.v1+json")))
                .body("""
                        {"schema": "{"type": "string"}"}
                        """)
                .retrieve().toEntity(String.class);

        assertThat(response)
                .extracting(ResponseEntity::getStatusCode, ResponseEntity::getBody)
                .contains(HttpStatus.OK, """
                        {"id":1}""");
    }
}
