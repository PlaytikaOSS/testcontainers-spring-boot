package com.playtika.testcontainer.vertica;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(
        classes = EmbeddedVerticaBootstrapConfigurationTest.TestConfiguration.class,
        properties = {
                "spring.profiles.active=enabled",
                "embedded.vertica.enabled=true"
        }
)
class EmbeddedVerticaBootstrapConfigurationTest {
    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ConfigurableEnvironment environment;

    @Test
    public void shouldConnectToVertica() {
        assertThat(jdbcTemplate.queryForObject("SELECT version()", String.class)).contains("Vertica Analytic Database");
    }

    @Test
    public void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.vertica.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.vertica.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.vertica.database")).isNotEmpty();
        assertThat(environment.getProperty("embedded.vertica.user")).isNotEmpty();
        assertThat(environment.getProperty("embedded.vertica.password")).isNotNull();
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}
