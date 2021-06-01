package com.playtika.test.prometheus;

import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.equalTo;

class EmbeddedPrometheusBootstrapConfigurationTest extends BaseEmbeddedPrometheusTest {

    @Test
    void shouldHaveMetrics() {
        UriComponents uriComponents =
                UriComponentsBuilder.newInstance()
                        .scheme("http")
                        .host(prometheusHost)
                        .port(prometheusPort)
                        .path("/api/v1/query?query=up")
                        .build();


        get(uriComponents.toUriString())
                .then()
                .assertThat()
                .body("status", equalTo("success"))
                .statusCode(200);
    }
}