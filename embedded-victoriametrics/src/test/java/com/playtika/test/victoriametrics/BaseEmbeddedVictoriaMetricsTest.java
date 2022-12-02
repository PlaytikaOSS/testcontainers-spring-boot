package com.playtika.test.victoriametrics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = BaseEmbeddedVictoriaMetricsTest.TestConfiguration.class,
        properties = "embedded.toxiproxy.proxies.victoriametrics.enabled=true")
public class BaseEmbeddedVictoriaMetricsTest {

    @Value("${embedded.victoriametrics.host}")
    protected String victoriaMetricsHost;
    @Value("${embedded.victoriametrics.port}")
    protected int victoriaMetricsPort;
    @Value("${embedded.victoriametrics.toxiproxy.host}")
    protected String victoriaMetricsToxiProxyHost;
    @Value("${embedded.victoriametrics.toxiproxy.port}")
    protected int victoriaMetricsToxiProxyPort;
    @Autowired
    protected ConfigurableListableBeanFactory beanFactory;

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}
