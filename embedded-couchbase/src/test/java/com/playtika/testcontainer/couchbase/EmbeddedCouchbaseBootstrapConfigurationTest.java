package com.playtika.testcontainer.couchbase;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = {EmbeddedCouchbaseBootstrapConfigurationTest.TestConfiguration.class},
        properties = {
                "spring.profiles.active=enabled",
                "embedded.toxiproxy.proxies.couchbase.enabled=true"
        })
public abstract class EmbeddedCouchbaseBootstrapConfigurationTest {

    @Value("${embedded.couchbase.bootstrapHttpDirectPort}")
    String couchbaseBootstrapHttpDirectPort;

    @Value("${embedded.couchbase.bootstrapCarrierDirectPort}")
    String couchbaseBootstrapCarrierDirectPort;

    @Value("${embedded.couchbase.host}")
    String couchbaseHost;

    @Value("${embedded.couchbase.bucket}")
    String couchbaseBucket;

    @Value("${embedded.couchbase.user}")
    String couchbaseUser;

    @Value("${embedded.couchbase.password}")
    String couchbasePassword;

    @Test
    public void propertiesAreAvailable() {
        assertThat(couchbaseBootstrapHttpDirectPort).isNotEmpty();
        assertThat(couchbaseBootstrapCarrierDirectPort).isNotEmpty();
        assertThat(couchbaseHost).isNotEmpty();
        assertThat(couchbaseBucket).isNotEmpty();
        assertThat(couchbaseUser).isNotEmpty();
        assertThat(couchbasePassword).isNotEmpty();
        assertThat(System.getProperty("com.couchbase.bootstrapHttpDirectPort")).isNotEmpty();
        assertThat(System.getProperty("com.couchbase.bootstrapCarrierDirectPort")).isNotEmpty();
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}
