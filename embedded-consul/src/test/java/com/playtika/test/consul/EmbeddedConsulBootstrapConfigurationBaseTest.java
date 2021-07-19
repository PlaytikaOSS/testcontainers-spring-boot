package com.playtika.test.consul;

import com.ecwid.consul.v1.ConsulClient;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.testcontainers.containers.GenericContainer;

public class EmbeddedConsulBootstrapConfigurationBaseTest {
    @Autowired
    protected ConfigurableEnvironment environment;

    @Autowired
    protected GenericContainer consulContainer;

    protected ConsulClient client;

    @BeforeEach
    private void init() {
        client = new ConsulClient(consulContainer.getHost(), consulContainer.getFirstMappedPort());
    }
}
