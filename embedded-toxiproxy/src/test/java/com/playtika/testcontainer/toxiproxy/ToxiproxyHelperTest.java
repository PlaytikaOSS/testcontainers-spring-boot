package com.playtika.testcontainer.toxiproxy;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.NetworkSettings;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ToxiproxyHelperTest {

    @Test
    void prefersExplicitAliasOverGeneratedTestcontainersAlias() {
        GenericContainer<?> container = mock(GenericContainer.class);
        when(container.getNetworkAliases()).thenReturn(List.of("tc-aM5u9Wsq", "minio.testcontainer.docker"));

        assertThat(ToxiproxyHelper.getUpstreamAddress(container, 9000))
                .isEqualTo("minio.testcontainer.docker:9000");
    }

    @Test
    void preservesCustomAliasStartingWithTcPrefix() {
        GenericContainer<?> container = mock(GenericContainer.class);
        when(container.getNetworkAliases()).thenReturn(List.of("tc-aM5u9Wsq", "tc-my-service"));

        assertThat(ToxiproxyHelper.getUpstreamAddress(container, 8080))
                .isEqualTo("tc-my-service:8080");
    }

    @Test
    void usesLiveContainerNetworkAliasWhenOnlyGeneratedAliasIsConfigured() {
        GenericContainer<?> container = mock(GenericContainer.class);
        when(container.getNetworkAliases()).thenReturn(List.of("tc-NewRun12"));

        ContainerNetwork bridgeNetwork = mock(ContainerNetwork.class);
        when(bridgeNetwork.getAliases()).thenReturn(null);
        when(bridgeNetwork.getIpAddress()).thenReturn("172.17.0.5");

        ContainerNetwork sharedNetwork = mock(ContainerNetwork.class);
        when(sharedNetwork.getAliases()).thenReturn(List.of("tc-OldRun99", "deadbeef1234"));
        when(sharedNetwork.getIpAddress()).thenReturn("172.18.0.5");

        NetworkSettings networkSettings = mock(NetworkSettings.class);
        when(networkSettings.getNetworks()).thenReturn(Map.of(
                "bridge", bridgeNetwork,
                "toxiproxy-net", sharedNetwork
        ));

        InspectContainerResponse containerInfo = mock(InspectContainerResponse.class);
        when(containerInfo.getNetworkSettings()).thenReturn(networkSettings);
        when(container.getContainerInfo()).thenReturn(containerInfo);

        assertThat(ToxiproxyHelper.getUpstreamAddress(container, 9000))
                .isEqualTo("tc-OldRun99:9000");
    }

    @Test
    void fallsBackToLiveContainerIpWhenNoAliasesExistOnRunningContainer() {
        GenericContainer<?> container = mock(GenericContainer.class);
        when(container.getNetworkAliases()).thenReturn(List.of());

        ContainerNetwork network = mock(ContainerNetwork.class);
        when(network.getAliases()).thenReturn(null);
        when(network.getIpAddress()).thenReturn("172.18.0.7");

        NetworkSettings networkSettings = mock(NetworkSettings.class);
        when(networkSettings.getNetworks()).thenReturn(Map.of("net", network));

        InspectContainerResponse containerInfo = mock(InspectContainerResponse.class);
        when(containerInfo.getNetworkSettings()).thenReturn(networkSettings);
        when(container.getContainerInfo()).thenReturn(containerInfo);

        assertThat(ToxiproxyHelper.getUpstreamAddress(container, 5432))
                .isEqualTo("172.18.0.7:5432");
    }

    @Test
    void fallsBackToGeneratedAliasWhenContainerNotYetStarted() {
        GenericContainer<?> container = mock(GenericContainer.class);
        when(container.getNetworkAliases()).thenReturn(List.of("tc-aM5u9Wsq"));
        when(container.getContainerInfo()).thenReturn(null);

        assertThat(ToxiproxyHelper.getUpstreamAddress(container, 9000))
                .isEqualTo("tc-aM5u9Wsq:9000");
    }

    @Test
    void throwsWhenNoAliasOrNetworkInfoAvailable() {
        GenericContainer<?> container = mock(GenericContainer.class);
        when(container.getNetworkAliases()).thenReturn(List.of());
        when(container.getContainerInfo()).thenReturn(null);

        assertThatThrownBy(() -> ToxiproxyHelper.getUpstreamAddress(container, 9000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot determine upstream address");
    }

    @Test
    void createProxyUsesExplicitNetworkAliasAsUpstream() throws Exception {
        ToxiproxyClient toxiproxyClient = mock(ToxiproxyClient.class);
        ToxiproxyContainer toxiproxyContainer = mock(ToxiproxyContainer.class);
        GenericContainer<?> targetContainer = mock(GenericContainer.class);
        Proxy proxy = mock(Proxy.class);

        when(targetContainer.getNetworkAliases()).thenReturn(List.of("tc-aM5u9Wsq", "minio.testcontainer.docker"));
        when(toxiproxyClient.createProxy(eq("minio"), anyString(), eq("minio.testcontainer.docker:9000")))
                .thenReturn(proxy);
        when(toxiproxyContainer.getHost()).thenReturn("localhost");
        when(toxiproxyContainer.getMappedPort(anyInt())).thenReturn(32768);

        ToxiproxyClientProxy result = ToxiproxyHelper.createProxy(
                toxiproxyClient, toxiproxyContainer, targetContainer, 9000, "minio");

        verify(toxiproxyClient).createProxy(eq("minio"), anyString(), eq("minio.testcontainer.docker:9000"));
        assertThat(result.getContainerIpAddress()).isEqualTo("localhost");
        assertThat(result.getProxyPort()).isEqualTo(32768);
    }
}
