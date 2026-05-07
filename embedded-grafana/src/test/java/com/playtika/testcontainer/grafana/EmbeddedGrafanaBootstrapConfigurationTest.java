package com.playtika.testcontainer.grafana;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import static io.restassured.RestAssured.given;

class EmbeddedGrafanaBootstrapConfigurationTest extends BaseEmbeddedGrafanaTest {
    @Value("${embedded.grafana.username}")
    private String username;
    @Value("${embedded.grafana.password}")
    private String password;
    @Value("${embedded.grafana.loki.port}")
    private int lokiPort;
    @Value("${embedded.grafana.tempo.port}")
    private int tempoPort;
    @Value("${embedded.grafana.otlp.http.port}")
    private int otlpHttpPort;

    @Test
    void grafanaApiShouldBeReachable() {
        UriComponents uriComponents = UriComponentsBuilder.newInstance()
                .scheme("http")
                .host(grafanaHost)
                .port(grafanaPort)
                .path("/api/health")
                .build();

        given()
                .auth()
                .preemptive()
                .basic(username, password)
                .get(uriComponents.toUriString())
                .then()
                .assertThat()
                .statusCode(200);
    }

    @Test
    void lokiShouldBeReachable() {
        UriComponents uriComponents = UriComponentsBuilder.newInstance()
                .scheme("http")
                .host(grafanaHost)
                .port(lokiPort)
                .path("/ready")
                .build();

        given()
                .get(uriComponents.toUriString())
                .then()
                .assertThat()
                .statusCode(200);
    }

    @Test
    void tempoShouldBeReachable() {
        UriComponents uriComponents = UriComponentsBuilder.newInstance()
                .scheme("http")
                .host(grafanaHost)
                .port(tempoPort)
                .path("/ready")
                .build();

        given()
                .get(uriComponents.toUriString())
                .then()
                .assertThat()
                .statusCode(200);
    }
}
