package com.playtika.test.grafana;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class EmbeddedGrafanaBootstrapConfigurationTest extends BaseEmbeddedGrafanaTest {
    @Value("${embedded.grafana.username}")
    private String username;
    @Value("${embedded.grafana.password}")
    private String password;

    @Test
    void shouldProvisionDatasourceFromConfigurationFile() {
        UriComponents uriComponents = UriComponentsBuilder.newInstance()
                .scheme("http")
                .host(grafanaHost)
                .port(grafanaPort)
                .path("/api/datasources/name/Prometheus")
                .build();

        given()
                .auth()
                .preemptive()
                .basic(username, password)
                .get(uriComponents.toUriString())
                .then()
                .assertThat()
                .body("url", equalTo("http://prometheus:9090"))
                .statusCode(200);
    }

}