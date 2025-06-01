package com.playtika.testcontainer.consul;

import com.ecwid.consul.v1.ConsulClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.testcontainers.containers.GenericContainer;

public class EmbeddedConsulBootstrapConfigurationBaseTest {
    @Value("${embedded.consul.host}")
    protected String consulHost;

    @Value("${embedded.consul.port}")
    protected String consulPort;

    @Value("${embedded.consul.enabled:false}")
    protected String consulEnabled;

    @Value("${embedded.consul.configurationFile:}")
    protected String consulConfigurationFile;

    @Autowired
    protected GenericContainer<?> consulContainer;

    protected ConsulClient buildClient() {
        return new ConsulClient(consulContainer.getHost(), consulContainer.getFirstMappedPort());
    }
}
