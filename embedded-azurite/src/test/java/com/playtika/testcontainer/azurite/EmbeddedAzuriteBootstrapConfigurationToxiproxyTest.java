package com.playtika.testcontainer.azurite;

import com.playtika.testcontainer.toxiproxy.ToxiproxyClientProxy;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.testcontainers.azure.AzuriteContainer;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmbeddedAzuriteBootstrapConfigurationToxiproxyTest {

    @Test
    void azuriteToxiproxyUsesUniqueProxyNamesAndRegistersQueueStoragePort() throws IOException {
        ToxiproxyClient toxiproxyClient = mock(ToxiproxyClient.class);
        ToxiproxyContainer toxiproxyContainer = mock(ToxiproxyContainer.class);
        AzuriteContainer azuriteContainer = mock(AzuriteContainer.class);
        MockEnvironment environment = new MockEnvironment();
        AzuriteProperties properties = new AzuriteProperties();

        when(azuriteContainer.getNetworkAliases()).thenReturn(List.of("azurite-blob.testcontainer.docker"));
        when(toxiproxyContainer.getMappedPort(anyInt())).thenReturn(18666);
        when(toxiproxyContainer.getHost()).thenReturn("localhost");
        when(toxiproxyClient.createProxy(anyString(), anyString(), anyString())).thenAnswer(invocation -> {
            Proxy proxy = mock(Proxy.class);
            when(proxy.getName()).thenReturn(invocation.getArgument(0));
            return proxy;
        });

        EmbeddedAzuriteBootstrapConfiguration configuration = new EmbeddedAzuriteBootstrapConfiguration();
        ToxiproxyClientProxy blobProxy = configuration.azuriteBlobContainerProxy(
                toxiproxyClient,
                toxiproxyContainer,
                azuriteContainer,
                properties,
                environment);
        ToxiproxyClientProxy queueProxy = configuration.azuriteQueueContainerProxy(
                toxiproxyClient,
                toxiproxyContainer,
                azuriteContainer,
                properties,
                environment);
        ToxiproxyClientProxy tableProxy = configuration.azuriteTableContainerProxy(
                toxiproxyClient,
                toxiproxyContainer,
                azuriteContainer,
                properties,
                environment);

        verify(toxiproxyClient).createProxy(eq("azurite-blob"), anyString(), eq("azurite-blob.testcontainer.docker:10000"));
        verify(toxiproxyClient).createProxy(eq("azurite-queue"), anyString(), eq("azurite-blob.testcontainer.docker:10001"));
        verify(toxiproxyClient).createProxy(eq("azurite-table"), anyString(), eq("azurite-blob.testcontainer.docker:10002"));
        assertThat(blobProxy.getName()).isEqualTo("azurite-blob");
        assertThat(queueProxy.getName()).isEqualTo("azurite-queue");
        assertThat(tableProxy.getName()).isEqualTo("azurite-table");
        assertThat(environment.getProperty("embedded.azurite.toxiproxy.blobStoragePort", Integer.class)).isEqualTo(18666);
        assertThat(environment.getProperty("embedded.azurite.toxiproxy.queueStoragePort", Integer.class)).isEqualTo(18666);
        assertThat(environment.getProperty("embedded.azurite.toxiproxy.tableStoragePort", Integer.class)).isEqualTo(18666);
    }
}
