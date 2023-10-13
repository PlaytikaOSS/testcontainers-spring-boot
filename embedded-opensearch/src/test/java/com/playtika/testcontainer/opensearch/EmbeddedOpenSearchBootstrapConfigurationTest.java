package com.playtika.testcontainer.opensearch;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.spring.boot.autoconfigure.RestClientBuilderCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.ConfigurableEnvironment;

import javax.net.ssl.SSLContext;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import static org.assertj.core.api.Assertions.assertThat;

@EnableAutoConfiguration(exclude = ElasticsearchDataAutoConfiguration.class)
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
                                    httpClientBuilder.setSSLContext(sslcontext());
                                }
                                return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider());
                            }
                    );
                }

                @Override
                public void customize(HttpAsyncClientBuilder builder) {
                    if (properties.isAllowInsecure()) {
                        builder.setSSLContext(sslcontext());
                    }
                    builder.setDefaultCredentialsProvider(credentialsProvider());
                }

                CredentialsProvider credentialsProvider() {
                    final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                    credentialsProvider.setCredentials(
                            AuthScope.ANY, new UsernamePasswordCredentials(properties.getUsername(), properties.getPassword())
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
