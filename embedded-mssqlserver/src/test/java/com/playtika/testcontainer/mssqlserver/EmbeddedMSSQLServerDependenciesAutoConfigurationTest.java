package com.playtika.testcontainer.mssqlserver;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = EmbeddedMSSQLServerDependenciesAutoConfigurationTest.TestConfiguration.class,
        properties = {
                "embedded.mssqlserver.enabled=true",
                "embedded.mssqlserver.password=Foobar1234!",
                "embedded.mssqlserver.accept-licence=true",
                "embedded.mssqlserver.init-script-path=initScript.sql"
        }
)
@ActiveProfiles("test")
class EmbeddedMSSQLServerDependenciesAutoConfigurationTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EmbeddedMSSQLServerContainer mssqlServerContainer;

    @Test
    void injectedJdbs() {
        assertThat(jdbcTemplate).isNotNull();
        assertThat(mssqlServerContainer.isRunning()).isTrue();
    }

    @Test
    @SneakyThrows
    void testCreateDb() {
        Map<String, Object> actual = jdbcTemplate.queryForMap("SELECT first_name, last_name FROM users WHERE first_name = 'Sam'");
        Map<String, Object> normalizedActual = actual.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().toLowerCase(Locale.ROOT),
                        Map.Entry::getValue,
                        (left, right) -> right,
                        LinkedHashMap::new
                ));

        Map<String, Object> expected = Map.of(
                "first_name", "Sam",
                "last_name", "Brannen"
        );

        assertThat(normalizedActual).isEqualTo(expected);
    }

    @EnableAutoConfiguration
    @Configuration
    @ImportAutoConfiguration({
            DataSourceAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class
    })
    static class TestConfiguration {
    }

}
