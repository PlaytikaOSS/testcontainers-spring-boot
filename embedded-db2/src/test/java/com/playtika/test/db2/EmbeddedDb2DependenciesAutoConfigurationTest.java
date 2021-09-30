package com.playtika.test.db2;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.testcontainers.containers.Db2Container;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = EmbeddedDb2DependenciesAutoConfigurationTest.TestConfiguration.class,
        properties = {
                "embedded.db2.enabled=true"
        }
)
class EmbeddedDb2DependenciesAutoConfigurationTest {

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Db2Container db2Container;

    @Test
    void injectedJdbs() {
        assertThat(jdbcTemplate).isNotNull();
    }

    @Test
    @SneakyThrows
    void testCreateDb() {
        Map<String, Object> map = jdbcTemplate.queryForMap("SELECT first_name, last_name FROM users WHERE first_name = 'Sam'");

        assertThat(map)
                .containsKey("first_name")
                .containsKey("last_name")
                .extractingByKey("last_name").isEqualTo("Brannen");
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }

}