package com.playtika.test.influxdb;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TestApplication.class})
class EmbeddedInfluxDBBootstrapConfigurationTest {

    @Autowired
    private InfluxDBProperties influxDBProperties;

    @Autowired
    private ConfigurableEnvironment environment;

    @Test
    void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.influxdb.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.influxdb.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.influxdb.database")).isNotEmpty();
        assertThat(environment.getProperty("embedded.influxdb.user")).isNotEmpty();
        assertThat(environment.getProperty("embedded.influxdb.password")).isNotEmpty();
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
}
