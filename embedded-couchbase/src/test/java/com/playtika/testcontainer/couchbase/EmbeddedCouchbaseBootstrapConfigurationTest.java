package com.playtika.testcontainer.couchbase;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = {EmbeddedCouchbaseBootstrapConfigurationTest.TestConfiguration.class},
        properties = {
                "spring.profiles.active=enabled",
                "embedded.toxiproxy.proxies.couchbase.enabled=true"
        })
public abstract class EmbeddedCouchbaseBootstrapConfigurationTest {

    @Autowired
    ConfigurableEnvironment environment;

    @Test
    public void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.couchbase.bootstrapHttpDirectPort")).isNotEmpty();
        assertThat(environment.getProperty("embedded.couchbase.bootstrapCarrierDirectPort")).isNotEmpty();
        assertThat(environment.getProperty("embedded.couchbase.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.couchbase.bucket")).isNotEmpty();
        assertThat(environment.getProperty("embedded.couchbase.user")).isNotEmpty();
        assertThat(environment.getProperty("embedded.couchbase.password")).isNotEmpty();
        assertThat(System.getProperty("com.couchbase.bootstrapHttpDirectPort")).isNotEmpty();
        assertThat(System.getProperty("com.couchbase.bootstrapCarrierDirectPort")).isNotEmpty();
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}
