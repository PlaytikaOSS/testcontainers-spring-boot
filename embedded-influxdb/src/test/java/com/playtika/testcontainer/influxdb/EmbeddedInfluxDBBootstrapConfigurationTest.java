package com.playtika.testcontainer.influxdb;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EmbeddedInfluxDBBootstrapConfigurationTest {


    @Value("${embedded.influxdb.port}")
    String influxdbPort;

    @Value("${embedded.influxdb.host}")
    String influxdbHost;

    @Value("${embedded.influxdb.database}")
    String influxdbDatabase;

    @Value("${embedded.influxdb.user}")
    String influxdbUser;

    @Value("${embedded.influxdb.password}")
    String influxdbPassword;

    @Test
    void propertiesAreAvailable() {
        assertThat(influxdbPort).isNotEmpty();
        assertThat(influxdbHost).isNotEmpty();
        assertThat(influxdbDatabase).isNotEmpty();
        assertThat(influxdbUser).isNotEmpty();
        assertThat(influxdbPassword).isNotEmpty();
    }

    @Test
    void influxDatabaseIsAvailable() {
        RestTemplate restTemplate = new RestTemplate();
        String baseUrl = "http://" + influxdbHost + ":" + influxdbPort;

        HttpHeaders headers = new HttpHeaders();
        String auth = influxdbUser + ":" + influxdbPassword;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + new String(encodedAuth, StandardCharsets.UTF_8);
        headers.set("Authorization", authHeader);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/ping",
                org.springframework.http.HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}
