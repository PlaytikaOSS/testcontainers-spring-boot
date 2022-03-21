package com.playtika.test.toxiproxy;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.Toxic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.testcontainers.containers.ToxiproxyContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = EmbeddedToxiProxyBootstrapConfigurationTest.TestConfiguration.class,
        properties = {
                "embedded.toxiproxy.enabled=true"
        }
)
class EmbeddedToxiProxyBootstrapConfigurationTest {

    @Autowired
    ConfigurableEnvironment environment;

    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @Value("${embedded.toxiproxy.host}")
    String host;

    @Value("${embedded.toxiproxy.controlPort}")
    int controlPort;


    @Test
    public void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.toxiproxy.controlPort")).isNotEmpty();
        assertThat(environment.getProperty("embedded.toxiproxy.host")).isNotEmpty();
    }

    @Test
    void containerIsAvailable() {
        assertThat(beanFactory.getBean("toxiproxy"))
                .isNotNull()
                .isInstanceOf(ToxiproxyContainer.class);
    }

    @Test
    void proxyIsAvailable() throws Exception {
        ToxiproxyClient toxiproxyClient = new ToxiproxyClient(host, controlPort);
        Proxy proxy = toxiproxyClient.createProxy("my-service-proxy", "0.0.0.0:8666", "any:any");

        List<? extends Toxic> all = proxy.toxics().getAll();

        assertThat(all).isEmpty();

        proxy.disable();

        assertThat(proxy.isEnabled()).isFalse();

        proxy.enable();

        assertThat(proxy.isEnabled()).isTrue();
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {

    }

}