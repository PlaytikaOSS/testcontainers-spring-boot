package com.playtika.test.artifactory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = EmbeddedArtifactoryDisabledTest.TestConfiguration.class,
        properties = "embedded.artifactory.enabled=false"
)
public class EmbeddedArtifactoryDisabledTest {

    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @Test
    public void contextLoads() {
        String[] containers = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, GenericContainer.class);
        assertThat(containers).isEmpty();
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {

    }
}
