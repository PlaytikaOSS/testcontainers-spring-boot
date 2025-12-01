package com.playtika.testcontainer.db2;

import lombok.SneakyThrows;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.db2.Db2Container;

import javax.sql.DataSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = EmbeddedDb2DependenciesAutoConfigurationTest.TestConfiguration.class,
        properties = {
                "embedded.db2.enabled=true"
        }
)
@ActiveProfiles("test")
class EmbeddedDb2DependenciesAutoConfigurationTest {

    @Autowired
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

        @Value("${spring.datasource.url}")
        private String jdbcUrl;
        @Value("${spring.datasource.username}")
        private String user;
        @Value("${spring.datasource.password}")
        private String password;

        @Bean(destroyMethod = "close")
        public DataSource customDatasource() {
            PoolConfiguration poolConfiguration = new PoolProperties();
            poolConfiguration.setUrl(jdbcUrl);
            poolConfiguration.setDriverClassName("com.ibm.db2.jcc.DB2Driver");
            poolConfiguration.setUsername(user);
            poolConfiguration.setPassword(password);
            poolConfiguration.setTestOnBorrow(true);
            poolConfiguration.setTestOnReturn(true);
            return new org.apache.tomcat.jdbc.pool.DataSource(poolConfiguration);
        }

        @Bean
        public JdbcTemplate jdbcTemplate(DataSource customDatasource) {
            return new JdbcTemplate(customDatasource);
        }
    }

}
