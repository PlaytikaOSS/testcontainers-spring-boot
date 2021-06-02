package com.playtika.test.grafana;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = BaseEmbeddedGrafanaTest.TestConfiguration.class
)
class BaseEmbeddedGrafanaTest {
    @Value("${embedded.grafana.host}")
    protected String grafanaHost;
    @Value("${embedded.grafana.port}")
    protected int grafanaPort;


    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {

    }

}