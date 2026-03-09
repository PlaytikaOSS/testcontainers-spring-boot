package com.playtika.testcontainer.consul;

import io.vertx.core.Vertx;
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.ConsulClientOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.testcontainers.containers.GenericContainer;

public class EmbeddedConsulBootstrapConfigurationBaseTest {
    @Autowired
    protected ConfigurableEnvironment environment;

    @Autowired
    protected GenericContainer<?> consulContainer;

    protected ConsulClient buildClient() {
        Vertx vertx = Vertx.builder().build();
        ConsulClientOptions options = new ConsulClientOptions()
                .setHost(consulContainer.getHost()).setPort(consulContainer.getFirstMappedPort());
        return ConsulClient.create(vertx, options);
    }
}
