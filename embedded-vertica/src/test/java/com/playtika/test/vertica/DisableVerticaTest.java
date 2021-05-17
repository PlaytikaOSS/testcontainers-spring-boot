package com.playtika.test.vertica;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(
        classes = DisableVerticaTest.TestConfiguration.class,
        properties = {
                "spring.profiles.active=disabled",
                "embedded.vertica.enabled=false"
        }
)
public class DisableVerticaTest {
    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @Test
    public void contextLoads() {
        String[] containers = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, GenericContainer.class);
        String[] postProcessors = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, BeanFactoryPostProcessor.class);

        assertThat(containers).isEmpty();
        assertThat(postProcessors).doesNotContain("datasourceVerticaDependencyPostProcessor");
    }

    @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
    @Configuration
    static class TestConfiguration {
    }
}
