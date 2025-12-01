package com.playtika.testcontainer.mysql;

import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static com.playtika.testcontainer.mysql.MySQLProperties.BEAN_NAME_EMBEDDED_MYSQL;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(
        classes = {
                AutoConfiguredDatasourceDependsOnTest.TestConfiguration.class
        },
        properties = {
                "embedded.toxiproxy.proxies.mysql.enabled=true"
        }
)
@ActiveProfiles("enabled")
public class AutoConfiguredDatasourceDependsOnTest {

    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    public void shouldConnectToMySQL() {
        assertThat(jdbcTemplate.queryForObject("select version()", String.class)).startsWith("9.1.");
    }

    @Test
    public void shouldSetupDependsOnForAllDataSources() {
        String[] beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, DataSource.class);
        assertThat(beanNamesForType)
                .as("Auto-configured datasource should be present")
                .hasSize(1)
                .contains("dataSource");
        asList(beanNamesForType).forEach(this::hasDependsOn);
    }

    private void hasDependsOn(String beanName) {
        assertThat(beanFactory.getBeanDefinition(beanName).getDependsOn())
                .isNotNull()
                .isNotEmpty()
                .contains(BEAN_NAME_EMBEDDED_MYSQL);
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

        @Bean(name = "dataSource", destroyMethod = "close")
        public DataSource dataSource() {
            PoolConfiguration poolConfiguration = new PoolProperties();
            poolConfiguration.setUrl(jdbcUrl);
            poolConfiguration.setDriverClassName("com.mysql.cj.jdbc.Driver");
            poolConfiguration.setUsername(user);
            poolConfiguration.setPassword(password);
            poolConfiguration.setTestOnBorrow(true);
            poolConfiguration.setTestOnReturn(true);
            return new org.apache.tomcat.jdbc.pool.DataSource(poolConfiguration);
        }

        @Bean
        public JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }
    }
}
