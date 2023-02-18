package com.playtika.test.couchbase;

import com.playtika.test.couchbase.springdata.CouchbaseConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = {CouchbaseConfiguration.class},
        properties = {
                "spring.profiles.active=enabled",
                "spring.main.allow-bean-definition-overriding=true",
                "embedded.couchbase.install.enabled=true"
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
}
