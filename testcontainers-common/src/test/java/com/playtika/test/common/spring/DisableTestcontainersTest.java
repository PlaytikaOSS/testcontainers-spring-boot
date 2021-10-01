package com.playtika.test.common.spring;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(classes = DisableTestcontainersTest.TestConfiguration.class,
        properties = "embedded.containers.enabled=false")
public class DisableTestcontainersTest {

    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @Test
    public void contextLoads() {
        String[] containers = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, GenericContainer.class);
        String[] postProcessors = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, BeanFactoryPostProcessor.class);
        String[] markers = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, DockerPresenceMarker.class);
        String[] allContainers = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, AllContainers.class);

        //test containers without property check
        assertThat(containers).contains("echoContainer1", "echoContainer2");
        assertThat(markers).isEmpty();
        assertThat(allContainers).isEmpty();
        assertThat(postProcessors).doesNotContain("containerDependsOnDockerPostProcessor");
        assertThat(postProcessors).doesNotContain("networkDependsOnDockerPostProcessor");
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}
