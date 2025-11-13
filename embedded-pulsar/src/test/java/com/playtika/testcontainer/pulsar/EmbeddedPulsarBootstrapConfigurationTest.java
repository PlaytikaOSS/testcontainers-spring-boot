package com.playtika.testcontainer.pulsar;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.testcontainers.containers.PulsarContainer;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddedPulsarBootstrapConfigurationTest extends AbstractEmbeddedPulsarTest {

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Autowired
    private ConfigurableEnvironment environment;

    @Test
    void propertiesShouldBeSet() {
        assertThat(environment.getProperty("embedded.pulsar.brokerUrl")).isNotEmpty();
        assertThat(environment.getProperty("embedded.pulsar.httpServiceUrl")).isNotEmpty();
    }

    @Test
    void pulsarContainerBeanShouldBeRegistered() {
        String[] beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, PulsarContainer.class);
        assertThat(beanNamesForType).hasSize(1);
    }
}
