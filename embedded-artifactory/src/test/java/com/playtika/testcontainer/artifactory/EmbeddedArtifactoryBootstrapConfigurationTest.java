package com.playtika.testcontainer.artifactory;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddedArtifactoryBootstrapConfigurationTest extends BaseEmbeddedArtifactoryTest {

    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    void shouldStartupArtifactory() {
        URI artifactoryUri = URI.create(String.format("http://%s:%d/", artifactoryHost, artifactoryPort));
        ResponseEntity<String> response = restTemplate.getForEntity(artifactoryUri, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

}
