package com.playtika.testcontainer.mysql;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

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
                "spring.profiles.active=enabled",
                "embedded.toxiproxy.proxies.mysql.enabled=true"
        }
)
public class AutoConfiguredDatasourceDependsOnTest {

    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    public void shouldConnectToMySQL() throws Exception {
        assertThat(jdbcTemplate.queryForObject("select version()", String.class)).startsWith("9.0.");
    }

    @Test
    public void shouldSetupDependsOnForAllDataSources() throws Exception {
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
    }
}
