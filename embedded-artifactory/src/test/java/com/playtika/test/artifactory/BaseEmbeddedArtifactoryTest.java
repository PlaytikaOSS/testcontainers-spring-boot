package com.playtika.test.artifactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = BaseEmbeddedArtifactoryTest.TestConfiguration.class
)
class BaseEmbeddedArtifactoryTest {
    @Value("${embedded.artifactory.host}")
    protected String artifactoryHost;
    @Value("${embedded.artifactory.port}")
    protected int artifactoryPort;


    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {

    }

}