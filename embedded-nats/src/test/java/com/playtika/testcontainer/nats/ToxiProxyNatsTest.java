package com.playtika.testcontainer.nats;

import eu.rekawek.toxiproxy.model.ToxicDirection;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.ToxiproxyContainer;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestPropertySource(properties = {
        "embedded.toxiproxy.proxies.nats.enabled=true"
})
class ToxiProxyNatsTest extends BaseNatsTest {

    @Autowired
    ToxiproxyContainer.ContainerProxy natsContainerProxy;

    @Autowired
    ConfigurableEnvironment environment;

    @Value("${embedded.nats.toxiproxy.host}")
    String host;
    @Value("${embedded.nats.toxiproxy.port}")
    int port;

    @Test
    void addsLatency() throws Exception {
        natsContainerProxy.toxics()
                .timeout("timeout", ToxicDirection.DOWNSTREAM, 5000);

        Options connectionOptions = getConnectionOptions(host, port, 1000);
        assertThatThrownBy(() -> Nats.connect(connectionOptions))
                .isInstanceOf(IOException.class);

        natsContainerProxy.toxics()
                .get("timeout").remove();
    }

    @Test
    void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.nats.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.nats.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.nats.toxiproxy.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.nats.toxiproxy.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.nats.toxiproxy.proxyName")).isNotEmpty();
    }

    private static Options getConnectionOptions(String host, int port, int timeoutInMillis) {
        return new Options.Builder()
                .server(String.format("nats://%s:%s", host, port))
                .connectionTimeout(Duration.ofMillis(timeoutInMillis))
                .maxReconnects(1)
                .build();
    }
}
