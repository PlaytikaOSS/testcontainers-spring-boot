package com.playtika.test.localstack;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.localstack.LocalStackContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = DisabledLocalStackTest.TestConfiguration.class,
        properties = "embedded.localstack.enabled=false")
public class DisabledLocalStackTest {
    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Test
    public void contextLoads() {
        String[] containers = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, LocalStackContainer.class);
        assertThat(containers).isEmpty();
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}
