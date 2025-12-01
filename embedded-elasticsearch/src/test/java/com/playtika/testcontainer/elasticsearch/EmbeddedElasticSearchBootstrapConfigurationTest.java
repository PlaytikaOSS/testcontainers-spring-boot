package com.playtika.testcontainer.elasticsearch;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("enabled")
@SpringBootTest(classes = EmbeddedElasticSearchBootstrapConfigurationTest.Config.class,
        properties = {
                "embedded.elasticsearch.install.enabled=true"
        })
public abstract class EmbeddedElasticSearchBootstrapConfigurationTest {

    @Value("${embedded.elasticsearch.clusterName}")
    String elasticsearchClusterName;

    @Value("${embedded.elasticsearch.host}")
    String elasticsearchHost;

    @Value("${embedded.elasticsearch.httpPort}")
    String elasticsearchHttpPort;

    @Value("${embedded.elasticsearch.transportPort}")
    String elasticsearchTransportPort;

    @Test
    public void propertiesAreAvailable() {
        assertThat(elasticsearchClusterName).isNotEmpty();
        assertThat(elasticsearchHost).isNotEmpty();
        assertThat(elasticsearchHttpPort).isNotEmpty();
        assertThat(elasticsearchTransportPort).isNotEmpty();
    }

    @Configuration
    @EnableAutoConfiguration
    @EnableElasticsearchRepositories(basePackages = "com.playtika.testcontainer.elasticsearch.springdata")
    public static class Config extends ElasticsearchConfiguration {

        @Value("${spring.elasticsearch.uris}")
        private String elasticsearchUri;

        @Override
        public ClientConfiguration clientConfiguration() {
            return ClientConfiguration.builder()
                    .connectedTo(elasticsearchUri.replace("http://", ""))
                    .build();
        }
    }

}
