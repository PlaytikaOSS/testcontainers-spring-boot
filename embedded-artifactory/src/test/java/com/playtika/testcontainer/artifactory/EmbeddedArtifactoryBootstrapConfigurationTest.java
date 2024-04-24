package com.playtika.testcontainer.artifactory;

import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import static io.restassured.RestAssured.given;

class EmbeddedArtifactoryBootstrapConfigurationTest extends BaseEmbeddedArtifactoryTest {

    @Test
    void shouldStartupArtifactory() {
        UriComponents uriComponents = UriComponentsBuilder.newInstance()
                .scheme("http")
                .host(artifactoryHost)
                .port(artifactoryPort)
                .path("/")
                .build();

        given()
                .get(uriComponents.toUriString())
                .then()
                .assertThat()
                .statusCode(200);
    }

}