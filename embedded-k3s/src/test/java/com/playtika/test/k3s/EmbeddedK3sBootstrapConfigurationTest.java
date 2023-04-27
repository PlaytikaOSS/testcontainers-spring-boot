package com.playtika.test.k3s;


import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        classes = {
                EmbeddedK3sBootstrapConfigurationTest.TestApplication.class
        }
)
class EmbeddedK3sBootstrapConfigurationTest {

    @Autowired
    private ConfigurableEnvironment environment;

    @Test
    void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.k3s.kubeconfig")).isNotEmpty();
    }

    @Test
    void k3sIsAvailable() {
        final var kubeconfig = environment.getProperty("embedded.k3s.kubeconfig");
        final var config = Config.fromKubeconfig(kubeconfig);

        try (final var client = new KubernetesClientBuilder().withConfig(config).build()) {
            assertFalse(client.nodes()
                    .list()
                    .getItems()
                    .isEmpty());
        }
    }

    @SpringBootApplication
    public static class TestApplication {
    }
}
