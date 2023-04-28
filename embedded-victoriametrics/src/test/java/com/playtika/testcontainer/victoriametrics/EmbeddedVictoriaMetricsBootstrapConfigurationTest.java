package com.playtika.testcontainer.victoriametrics;

import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.equalTo;

public class EmbeddedVictoriaMetricsBootstrapConfigurationTest extends BaseEmbeddedVictoriaMetricsTest {

    @Test
    void shouldHaveMetrics() {

        UriComponents uriComponents =
                UriComponentsBuilder.newInstance()
                        .scheme("http")
                        .host(victoriaMetricsHost)
                        .port(victoriaMetricsPort)
                        .path("/api/v1/query?query=up")
                        .build();

        get(uriComponents.toUriString())
                .then()
                .assertThat()
                .body("status", equalTo("success"))
                .statusCode(200);
    }
}
