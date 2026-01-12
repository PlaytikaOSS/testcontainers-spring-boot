package com.playtika.testcontainer.k3s;


import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class EmbeddedK3sBootstrapConfigurationTest {

    @Value("${embedded.k3s.kubeconfig}")
    private String k3sKubeconfig;

    @Test
    void propertiesAreAvailable() {
        assertThat(k3sKubeconfig).isNotEmpty();
    }

    @Test
    void k3sIsAvailable() {
        final var config = Config.fromKubeconfig(k3sKubeconfig);

        try (final var client = new KubernetesClientBuilder().withConfig(config).build()) {
            assertFalse(client.nodes()
                    .list()
                    .getItems()
                    .isEmpty());
        }
    }

    @Configuration
    static class TestConfiguration {
    }
}
