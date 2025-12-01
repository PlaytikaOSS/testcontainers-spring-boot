package com.playtika.testcontainer.opensearch;

import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.junit.jupiter.api.Test;
import org.opensearch.data.client.osc.OpenSearchConfiguration;
import org.opensearch.spring.boot.autoconfigure.OpenSearchClientAutoConfiguration;
import org.opensearch.spring.boot.autoconfigure.OpenSearchRestClientAutoConfiguration;
import org.opensearch.spring.boot.autoconfigure.OpenSearchRestHighLevelClientAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;

@EnableAutoConfiguration(exclude = {
        OpenSearchClientAutoConfiguration.class,
        OpenSearchRestClientAutoConfiguration.class,
        OpenSearchRestHighLevelClientAutoConfiguration.class
})
public abstract class EmbeddedOpenSearchBootstrapConfigurationTest {

    @Autowired
    protected ConfigurableEnvironment environment;

    @Test
    public void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.opensearch.clusterName")).isNotEmpty();
        assertThat(environment.getProperty("embedded.opensearch.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.opensearch.httpPort")).isNotEmpty();
        assertThat(environment.getProperty("embedded.opensearch.transportPort")).isNotEmpty();
        assertThat(environment.getProperty("opensearch.uris")).isNotEmpty();
    }

    @Configuration
    @EnableAutoConfiguration(exclude = {
            OpenSearchClientAutoConfiguration.class,
            OpenSearchRestClientAutoConfiguration.class,
            OpenSearchRestHighLevelClientAutoConfiguration.class
    })
    @EnableElasticsearchRepositories(basePackages = "com.playtika.testcontainer.opensearch.springdata")
    public static class Config extends OpenSearchConfiguration {

        @Value("${embedded.opensearch.host}")
        private String opensearchHost;

        @Value("${embedded.opensearch.httpPort}")
        private int opensearchPort;

        @Autowired
        private OpenSearchProperties openSearchProperties;

        @Override
        public ClientConfiguration clientConfiguration() {
            String hostAndPort = opensearchHost + ":" + opensearchPort;

            if (openSearchProperties.isCredentialsEnabled()) {
                try {
                    HostnameVerifier allHostsValid = (hostname, session) -> true;
                    return ClientConfiguration.builder()
                            .connectedTo(hostAndPort)
                            .usingSsl(sslcontext(), allHostsValid)
                            .withBasicAuth(openSearchProperties.getUsername(), openSearchProperties.getPassword())
                            .build();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create SSL context", e);
                }
            }

            return ClientConfiguration.builder()
                    .connectedTo(hostAndPort)
                    .build();
        }

        private SSLContext sslcontext() throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
            return SSLContextBuilder.create()
                    .loadTrustMaterial(null, (TrustStrategy) (X509Certificate[] chain, String authType) -> true)
                    .build();
        }
    }
}
