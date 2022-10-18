package com.playtika.test.victoriametrics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = BaseEmbeddedVictoriaMetricsTest.TestConfiguration.class)
public class BaseEmbeddedVictoriaMetricsTest {

    @Value("${embedded.victoriametrics.host}")
    protected String victoriaMetricsHost;
    @Value("${embedded.victoriametrics.port}")
    protected int victoriaMetricsPort;
    @Autowired
    protected ConfigurableListableBeanFactory beanFactory;

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}
