package com.playtika.testcontainer.victoriametrics;

import eu.rekawek.toxiproxy.model.ToxicDirection;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.ToxiproxyContainer;

import java.io.IOException;
import java.net.SocketTimeoutException;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;

public class ToxiProxyVictoriaMetricsTest extends BaseEmbeddedVictoriaMetricsTest {

    @Autowired
    private ToxiproxyContainer.ContainerProxy victoriaMetricsContainerProxy;

    @Test
    void shouldAddLatency() throws IOException {

        UriComponents uriComponents =
                UriComponentsBuilder.newInstance()
                        .scheme("http")
                        .host(victoriaMetricsToxiProxyHost)
                        .port(victoriaMetricsToxiProxyPort)
                        .path("/api/v1/query?query=up")
                        .build();

        victoriaMetricsContainerProxy.toxics()
                .latency("latency", ToxicDirection.DOWNSTREAM, 1_100)
                .setJitter(100);

        RestAssuredConfig config = RestAssured.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.socket.timeout", 200));

        assertThatThrownBy(() -> given()
                .config(config)
                .get(uriComponents.toUriString()))
                .isInstanceOf(SocketTimeoutException.class);

        victoriaMetricsContainerProxy.toxics()
                .get("latency").remove();

        given()
                .config(config)
                .get(uriComponents.toUriString())
                .then()
                .assertThat()
                .body("status", equalTo("success"))
                .statusCode(200);
    }
}
