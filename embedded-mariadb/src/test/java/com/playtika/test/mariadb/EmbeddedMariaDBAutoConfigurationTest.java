package com.playtika.test.mariadb;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = EmbeddedMariaDBAutoConfigurationTest.TestConfiguration.class)
public class EmbeddedMariaDBAutoConfigurationTest {

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ConfigurableEnvironment environment;

    @Test
    public void shouldConnectToMariaDB() throws Exception {
        assertThat(jdbcTemplate.queryForObject("select version()", String.class)).contains("MariaDB");
    }

    @Test
    public void propertiesAreAvalable() {
        assertThat(environment.getProperty("embedded.mariadb.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.mariadb.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.mariadb.schema")).isNotEmpty();
        assertThat(environment.getProperty("embedded.mariadb.user")).isNotEmpty();
        assertThat(environment.getProperty("embedded.mariadb.password")).isNotEmpty();
    }
}
