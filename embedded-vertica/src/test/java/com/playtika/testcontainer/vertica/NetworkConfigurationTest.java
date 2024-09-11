package com.playtika.testcontainer.vertica;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.util.Objects;

import static com.playtika.testcontainer.vertica.VerticaProperties.BEAN_NAME_EMBEDDED_VERTICA;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class NetworkConfigurationTest {

    @Nested
    @SpringBootTest(
        classes = TestConfiguration.class,
        properties = {
            "spring.profiles.active=enabled",
            "embedded.vertica.enabled=true",
            "embedded.vertica.custom-network=true"
        }
    )
    class CustomNetworkConfigurationTest {

        @Autowired
        @Qualifier(BEAN_NAME_EMBEDDED_VERTICA)
        private GenericContainer<?> embeddedVertica;

        @Autowired
        private Network network;

        @Test
        public void shouldHaveCustomNetworkRegisteredForContainer() {
            assertThat(embeddedVertica.getNetwork()).isSameAs(network);
            assertThat(embeddedVertica.getContainerInfo().getNetworkSettings().getNetworks()).hasSize(1);
            assertThat(embeddedVertica.getContainerInfo().getNetworkSettings().getNetworks())
                .hasValueSatisfying(new Condition<>(n -> Objects.equals(n.getNetworkID(), network.getId()), "Network with the same id"));
        }

    }

    @Nested
    @SpringBootTest(
        classes = TestConfiguration.class,
        properties = {
            "spring.profiles.active=enabled",
        }
    )
    class DefaultNetworkConfigurationTest {

        @Autowired
        @Qualifier(BEAN_NAME_EMBEDDED_VERTICA)
        private GenericContainer<?> embeddedVertica;

        @Test
        public void shouldHaveSharedNetworkRegisteredForContainer() {
            assertThat(embeddedVertica.getNetwork()).isSameAs(Network.SHARED);
            assertThat(embeddedVertica.getContainerInfo().getNetworkSettings().getNetworks()).hasSize(1);
            assertThat(embeddedVertica.getContainerInfo().getNetworkSettings().getNetworks())
                .hasValueSatisfying(new Condition<>(n -> Objects.equals(n.getNetworkID(), Network.SHARED.getId()), "Network with the same id"));
        }

    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {

    }

}
