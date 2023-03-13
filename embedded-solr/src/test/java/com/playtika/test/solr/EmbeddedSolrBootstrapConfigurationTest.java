package com.playtika.test.solr;

import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class EmbeddedSolrBootstrapConfigurationTest extends BaseSolrTest {

    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @Autowired
    ConfigurableEnvironment environment;

    @Autowired
    SolrClient solrClient;

    @Test
    void shouldConnect() throws SolrServerException, IOException {
        assertThat(solrClient.ping("dummy").getStatus()).isEqualTo(0);
    }

    @Test
    void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.solr.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.solr.host")).isNotEmpty();
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}
