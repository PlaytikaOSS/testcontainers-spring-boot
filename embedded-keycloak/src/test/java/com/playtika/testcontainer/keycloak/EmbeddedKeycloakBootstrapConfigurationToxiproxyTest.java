package com.playtika.testcontainer.keycloak;

import com.playtika.testcontainer.toxiproxy.ToxiproxyClientProxy;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmbeddedKeycloakBootstrapConfigurationToxiproxyTest {

    @Test
    void keycloakToxiproxyUsesInternalHttpPortForDockerNetworkUpstream() throws IOException {
        ToxiproxyClient toxiproxyClient = mock(ToxiproxyClient.class);
        ToxiproxyContainer toxiproxyContainer = mock(ToxiproxyContainer.class);
        KeycloakContainer keycloakContainer = mock(KeycloakContainer.class);
        Proxy proxy = mock(Proxy.class);
        MockEnvironment environment = new MockEnvironment();

        when(keycloakContainer.getNetworkAliases()).thenReturn(List.of("keycloak.testcontainer.docker"));
        when(toxiproxyContainer.getMappedPort(anyInt())).thenReturn(18666);
        when(toxiproxyContainer.getHost()).thenReturn("localhost");
        when(proxy.getName()).thenReturn("keycloak");
        when(toxiproxyClient.createProxy(eq("keycloak"), anyString(), eq("keycloak.testcontainer.docker:8080")))
                .thenReturn(proxy);

        ToxiproxyClientProxy clientProxy = new EmbeddedKeycloakBootstrapConfiguration().keycloakContainerProxy(
                toxiproxyClient,
                toxiproxyContainer,
                keycloakContainer,
                new KeycloakProperties(),
                environment);

        verify(toxiproxyClient).createProxy(eq("keycloak"), anyString(), eq("keycloak.testcontainer.docker:8080"));
        verify(keycloakContainer, never()).getHttpPort();
        assertThat(clientProxy.getProxyPort()).isEqualTo(18666);
        assertThat(environment.getProperty("embedded.keycloak.toxiproxy.host")).isEqualTo("localhost");
        assertThat(environment.getProperty("embedded.keycloak.toxiproxy.port", Integer.class)).isEqualTo(18666);
    }
}
