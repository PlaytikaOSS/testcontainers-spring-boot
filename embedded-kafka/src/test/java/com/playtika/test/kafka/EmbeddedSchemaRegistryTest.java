/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Playtika
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.playtika.test.kafka;

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

class EmbeddedSchemaRegistryTest extends AbstractEmbeddedKafkaTest {

    @Value("${embedded.kafka.schema-registry.host}")
    private String host;

    @Value("${embedded.kafka.schema-registry.port}")
    private Integer port;

    @Test
    void schemasTopicAvailable() throws Exception {
        assertThatTopicExists("_schemas");
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
