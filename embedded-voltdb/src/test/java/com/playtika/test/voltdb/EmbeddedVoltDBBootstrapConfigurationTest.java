package com.playtika.test.voltdb;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.CallableStatement;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(classes = EmbeddedVoltDBBootstrapConfigurationTest.TestConfiguration.class)
@ActiveProfiles("enabled")
public class EmbeddedVoltDBBootstrapConfigurationTest {

    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ConfigurableEnvironment environment;

    @Test
    public void shouldConnect() {
        Map<String, Object> result = jdbcTemplate.call(con -> {
            CallableStatement call = con.prepareCall("{call @SystemInformation(?)}");
            call.setString(1, "overview");
            return call;
        }, Collections.emptyList());
        assertThat(result).isNotEmpty();
    }

    @Test
    public void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.voltdb.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.voltdb.host")).isNotEmpty();
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}
