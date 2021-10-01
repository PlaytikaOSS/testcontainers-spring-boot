package com.playtika.test.elasticsearch;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("enabled")
@SpringBootTest(properties = "embedded.elasticsearch.install.enabled=true"
        , classes = EmbeddedElasticSearchBootstrapConfigurationTest.Config.class)
public abstract class EmbeddedElasticSearchBootstrapConfigurationTest {

    @Autowired
    private ConfigurableEnvironment environment;

    @Test
    public void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.elasticsearch.clusterName")).isNotEmpty();
        assertThat(environment.getProperty("embedded.elasticsearch.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.elasticsearch.httpPort")).isNotEmpty();
        assertThat(environment.getProperty("embedded.elasticsearch.transportPort")).isNotEmpty();
    }

    @Configuration
    @EnableAutoConfiguration
    public static class Config {
    }

}