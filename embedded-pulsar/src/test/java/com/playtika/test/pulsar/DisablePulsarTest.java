package com.playtika.test.pulsar;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PulsarContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AbstractEmbeddedPulsarTest.TestConfiguration.class,
        properties = "embedded.pulsar.enabled=false")
class DisablePulsarTest {

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Test
    void shouldNotRegisterBean() {
        String[] beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, PulsarContainer.class);
        assertThat(beanNamesForType).isEmpty();
    }
}
