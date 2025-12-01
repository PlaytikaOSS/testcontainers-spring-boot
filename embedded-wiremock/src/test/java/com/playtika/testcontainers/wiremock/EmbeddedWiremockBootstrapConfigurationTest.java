package com.playtika.testcontainers.wiremock;

import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

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
    }

    @Test
    void shouldRequestWiremockStub() {
        stubFor(get("/say-hello")
                .willReturn(ok("Hello world!")));

        RestTemplate restTemplate = new RestTemplate();
        String baseUrl = "http://" + wiremockHost + ":" + wiremockPort;
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/say-hello", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Hello world!");
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
