package com.playtika.test.pubsub;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.testcontainers.containers.GenericContainer;

@Slf4j
@SpringBootTest(
        classes = DisablePubsubTest.TestConfiguration.class,
        properties = {"embedded.google.pubsub.enabled=false", "spring.cloud.gcp.pubsub.enabled=false"})
public class DisablePubsubTest {
    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Test
    public void propertiesAreNotAvailable() {
        assertThat(environment.getProperty("embedded.google.pubsub.port")).isNullOrEmpty();
        assertThat(environment.getProperty("embedded.google.pubsub.host")).isNullOrEmpty();
        assertThat(environment.getProperty("embedded.google.pubsub.project-id")).isNullOrEmpty();
    }

    @Test
    public void contextLoads() {
        String[] containers = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, GenericContainer.class);
        String[] postProcessors = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, BeanFactoryPostProcessor.class);

        assertThat(containers).isEmpty();
        assertThat(postProcessors).doesNotContain("pubsubTemplateDependencyPostProcessor");
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}