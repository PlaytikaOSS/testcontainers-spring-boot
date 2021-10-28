package com.playtika.test.consul;

import com.ecwid.consul.v1.ConsulClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.testcontainers.containers.GenericContainer;

public class EmbeddedConsulBootstrapConfigurationBaseTest {
    @Autowired
    protected ConfigurableEnvironment environment;

    @Autowired
    protected GenericContainer consulContainer;

    protected ConsulClient buildClient() {
        return new ConsulClient(consulContainer.getHost(), consulContainer.getFirstMappedPort());
    }
}
