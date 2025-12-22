package com.playtika.testcontainer.opensearch;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.URIScheme;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.spring.boot.autoconfigure.RestClientBuilderCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.elasticsearch.autoconfigure.DataElasticsearchAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.ConfigurableEnvironment;

import javax.net.ssl.SSLContext;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import static org.assertj.core.api.Assertions.assertThat;

@EnableAutoConfiguration(exclude = DataElasticsearchAutoConfiguration.class)
public abstract class EmbeddedOpenSearchBootstrapConfigurationTest {

    @Autowired
    protected ConfigurableEnvironment environment;

    @Test
    public void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.opensearch.clusterName")).isNotEmpty();
        assertThat(environment.getProperty("embedded.opensearch.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.opensearch.httpPort")).isNotEmpty();
        assertThat(environment.getProperty("embedded.opensearch.transportPort")).isNotEmpty();
    }

    @Configuration
    @EnableAutoConfiguration
    public static class Config {

        @Bean
        @Profile("credentials")
        public RestClientBuilderCustomizer restClientBuilderCustomizer(@NotNull OpenSearchProperties properties) {
            return new RestClientBuilderCustomizer() {

                @Override
                public void customize(RestClientBuilder builder) {
                    builder.setHttpClientConfigCallback(
                            httpClientBuilder -> {
                                if (properties.isAllowInsecure()) {
                                    httpClientBuilder.setConnectionManager(new PoolingAsyncClientConnectionManager(RegistryBuilder.<TlsStrategy>create().register(URIScheme.HTTPS.getId(),new DefaultClientTlsStrategy(sslcontext())).build()));
                                }
                                return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider());
                            }
                    );
                }

                @Override
                public void customize(HttpAsyncClientBuilder builder) {
                    if (properties.isAllowInsecure()) {
                        builder.setConnectionManager(new PoolingAsyncClientConnectionManager(RegistryBuilder.<TlsStrategy>create().register(URIScheme.HTTPS.getId(),new DefaultClientTlsStrategy(sslcontext())).build()));
                    }
                    builder.setDefaultCredentialsProvider(credentialsProvider());
                }

                CredentialsProvider credentialsProvider() {
                    final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                    credentialsProvider.setCredentials(
                            new AuthScope(null, -1), new UsernamePasswordCredentials(properties.getUsername(), properties.getPassword().toCharArray())
                    );
                    return credentialsProvider;
                }

                SSLContext sslcontext() {
                    try {
                        return SSLContextBuilder.create()
                                .loadTrustMaterial(null, new TrustAllStrategy())
                                .build();
                    } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        }
    }
}
