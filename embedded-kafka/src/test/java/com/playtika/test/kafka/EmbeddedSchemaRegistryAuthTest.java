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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

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
