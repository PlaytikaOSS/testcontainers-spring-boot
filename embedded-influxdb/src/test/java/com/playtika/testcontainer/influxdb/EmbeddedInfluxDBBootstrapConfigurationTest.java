package com.playtika.testcontainer.influxdb;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class EmbeddedInfluxDBBootstrapConfigurationTest {

    @Autowired
    private InfluxDBProperties influxDBProperties;

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
        RestAssured.baseURI = getUrl();
        ExtractableResponse response = given()
                .get("/ping")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT)
                .extract();

        assertThat(response.statusCode()).isEqualTo(204);
    }

    private String getUrl() {
        return "http://" + influxDBProperties.getHost() + ":" + influxDBProperties.getPort();
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}
