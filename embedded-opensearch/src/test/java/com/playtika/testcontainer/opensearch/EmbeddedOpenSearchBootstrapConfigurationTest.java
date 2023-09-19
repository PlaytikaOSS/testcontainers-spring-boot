package com.playtika.testcontainer.opensearch;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("enabled")
@EnableAutoConfiguration(exclude = ElasticsearchDataAutoConfiguration.class)
@SpringBootTest(
        classes = EmbeddedOpenSearchBootstrapConfigurationTest.Config.class,
        properties = {
                "embedded.opensearch.install.enabled=true"
        })
public abstract class EmbeddedOpenSearchBootstrapConfigurationTest {

    @Autowired
    private ConfigurableEnvironment environment;

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

    }

}