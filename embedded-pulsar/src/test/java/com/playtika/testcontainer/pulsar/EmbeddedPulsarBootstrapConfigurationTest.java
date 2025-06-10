package com.playtika.testcontainer.pulsar;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.testcontainers.containers.PulsarContainer;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddedPulsarBootstrapConfigurationTest extends AbstractEmbeddedPulsarTest {

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Value("${embedded.pulsar.brokerUrl}")
    String pulsarBrokerUrl;

    @Value("${embedded.pulsar.httpServiceUrl}")
    String pulsarHttpServiceUrl;

    @Test
    void propertiesShouldBeSet() {
        assertThat(pulsarBrokerUrl).isNotEmpty();
        assertThat(pulsarHttpServiceUrl).isNotEmpty();
    }

    @Test
    void pulsarContainerBeanShouldBeRegistered() {
        String[] beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, PulsarContainer.class);
        assertThat(beanNamesForType).hasSize(1);
    }
}
