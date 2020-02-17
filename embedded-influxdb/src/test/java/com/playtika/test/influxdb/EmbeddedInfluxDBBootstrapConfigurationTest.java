/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Playtika
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
