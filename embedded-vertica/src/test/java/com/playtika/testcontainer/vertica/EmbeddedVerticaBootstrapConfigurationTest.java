package com.playtika.testcontainer.vertica;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ActiveProfiles("enabled")
@SpringBootTest(
        classes = EmbeddedVerticaBootstrapConfigurationTest.TestConfiguration.class,
        properties = {
                "embedded.vertica.enabled=true"
        }
)
class EmbeddedVerticaBootstrapConfigurationTest {
    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Value("${embedded.vertica.port}")
    String verticaPort;

    @Value("${embedded.vertica.host}")
    String verticaHost;

    @Value("${embedded.vertica.database}")
    String verticaDatabase;

    @Value("${embedded.vertica.user}")
    String verticaUser;

    @Value("${embedded.vertica.password}")
    String verticaPassword;

    @Test
    public void shouldConnectToVertica() {
        assertThat(jdbcTemplate.queryForObject("SELECT version()", String.class)).contains("Vertica Analytic Database");
    }

    @Test
    public void propertiesAreAvailable() {
        assertThat(verticaPort).isNotEmpty();
        assertThat(verticaHost).isNotEmpty();
        assertThat(verticaDatabase).isNotEmpty();
        assertThat(verticaUser).isNotEmpty();
        assertThat(verticaPassword).isNotNull();
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}
