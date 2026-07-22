package com.playtika.testcontainer.couchbase;

import com.playtika.testcontainer.toxiproxy.ToxiproxyClientProxy;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.testcontainers.couchbase.CouchbaseContainer;
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

class EmbeddedCouchbaseBootstrapConfigurationToxiproxyTest {

    @Test
    void couchbaseToxiproxyUsesInternalHttpPortForDockerNetworkUpstream() throws IOException {
        ToxiproxyClient toxiproxyClient = mock(ToxiproxyClient.class);
        ToxiproxyContainer toxiproxyContainer = mock(ToxiproxyContainer.class);
        CouchbaseContainer couchbaseContainer = mock(CouchbaseContainer.class);
        Proxy proxy = mock(Proxy.class);
        MockEnvironment environment = new MockEnvironment();

        when(couchbaseContainer.getNetworkAliases()).thenReturn(List.of("couchbase.testcontainer.docker"));
        when(toxiproxyContainer.getMappedPort(anyInt())).thenReturn(18666);
        when(toxiproxyContainer.getHost()).thenReturn("localhost");
        when(proxy.getName()).thenReturn("couchbase");
        when(toxiproxyClient.createProxy(eq("couchbase"), anyString(), eq("couchbase.testcontainer.docker:8091")))
                .thenReturn(proxy);

        ToxiproxyClientProxy clientProxy = new EmbeddedCouchbaseBootstrapConfiguration().couchbaseContainerProxy(
                toxiproxyClient,
                toxiproxyContainer,
                couchbaseContainer,
                environment);

        verify(toxiproxyClient).createProxy(eq("couchbase"), anyString(), eq("couchbase.testcontainer.docker:8091"));
        verify(couchbaseContainer, never()).getBootstrapHttpDirectPort();
        assertThat(clientProxy.getProxyPort()).isEqualTo(18666);
        assertThat(environment.getProperty("embedded.couchbase.toxiproxy.host")).isEqualTo("localhost");
        assertThat(environment.getProperty("embedded.couchbase.toxiproxy.port", Integer.class)).isEqualTo(18666);
    }
}
