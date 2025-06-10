package com.playtika.testcontainers.wiremock;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Slf4j
@SpringBootTest(
        classes = EmbeddedWiremockBootstrapConfigurationTest.TestConfiguration.class,
        properties = {
                "embedded.wiremock.enabled=true"
        }
)
public class EmbeddedWiremockBootstrapConfigurationTest {

    @Value("${embedded.wiremock.host}")
    String wiremockHost;

    @Value("${embedded.wiremock.port}")
    int wiremockPort;

    @Value("${embedded.wiremock.networkAlias}")
    String wiremockNetworkAlias;

    @Value("${embedded.wiremock.internalPort}")
    String wiremockInternalPort;

    @BeforeEach
    void setUp() {
        WireMock.configureFor(wiremockHost, wiremockPort);
        RestAssured.port = wiremockPort;
    }

    @Test
    void shouldRequestWiremockStub() {
        stubFor(get("/say-hello")
                .willReturn(ok("Hello world!")));

        given()
                .get("/say-hello")
                .then()
                .assertThat()
                .log().all()
                .statusCode(200)
                .body(equalTo("Hello world!"));
    }

    @Test
    public void propertiesAreAvailable() {
        assertThat(String.valueOf(wiremockPort)).isNotEmpty();
        assertThat(wiremockHost).isNotEmpty();
        assertThat(wiremockNetworkAlias).isNotEmpty();
        assertThat(wiremockInternalPort).isNotEmpty();
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}
