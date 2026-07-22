package com.playtika.testcontainer.grafana;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.grafana.LgtmStackContainer;

import java.io.IOException;

import static com.playtika.testcontainer.grafana.GrafanaProperties.GRAFANA_BEAN_NAME;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("anonymous")
@DirtiesContext
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = EmbeddedGrafanaAnonymousAuthTest.TestConfiguration.class)
class EmbeddedGrafanaAnonymousAuthTest {

    @Value("${embedded.grafana.host}")
    private String grafanaHost;
    @Value("${embedded.grafana.port}")
    private int grafanaPort;

    @Autowired
    @Qualifier(GRAFANA_BEAN_NAME)
    private LgtmStackContainer grafanaContainer;

    @Test
    void anonymousAuthEnvVarsShouldBeSetOnContainer() throws IOException, InterruptedException {
        var result = grafanaContainer.execInContainer("sh", "-c", "env");
        assertThat(result.getStdout())
                .contains("GF_AUTH_ANONYMOUS_ENABLED=true")
                .contains("GF_AUTH_ANONYMOUS_ORG_ROLE=Viewer");
    }

    @Test
    void grafanaApiShouldBeReachableWithoutCredentials() {
        given()
                .get(url("/api/health"))
                .then()
                .statusCode(200);
    }

    @Test
    void authenticatedEndpointShouldBeAccessibleWithoutCredentials() {
        given()
                .get(url("/api/dashboards/home"))
                .then()
                .statusCode(200);
    }

    private String url(String path) {
        return UriComponentsBuilder.newInstance()
                .scheme("http")
                .host(grafanaHost)
                .port(grafanaPort)
                .path(path)
                .build()
                .toUriString();
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {}
}
