package com.playtika.testcontainer.mockserver;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MockServerContainer;

import static org.assertj.core.api.Assertions.assertThat;

class MockServerContainerConfigurationTest {

    private final EmbeddedMockServerBootstrapConfiguration configuration = new EmbeddedMockServerBootstrapConfiguration();

    @Test
    void shouldPassServerPortAsSeparateCommandArguments() {
        MockServerProperties properties = new MockServerProperties();
        properties.setPort(1090);

        MockServerContainer mockServerContainer = configuration.createMockServerContainer(properties);

        assertThat(mockServerContainer.getCommandParts()).containsExactly("-serverPort", "1090");
    }
}
