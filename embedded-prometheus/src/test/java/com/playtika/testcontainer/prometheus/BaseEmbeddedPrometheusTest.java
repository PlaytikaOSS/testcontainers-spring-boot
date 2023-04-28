package com.playtika.testcontainer.prometheus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = BaseEmbeddedPrometheusTest.TestConfiguration.class
)
class BaseEmbeddedPrometheusTest {
    @Value("${embedded.prometheus.host}")
    protected String prometheusHost;
    @Value("${embedded.prometheus.port}")
    protected int prometheusPort;
    @Autowired
    protected ConfigurableListableBeanFactory beanFactory;

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {

    }

}