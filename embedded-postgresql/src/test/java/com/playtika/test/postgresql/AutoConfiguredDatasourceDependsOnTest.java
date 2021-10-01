package com.playtika.test.postgresql;

import com.playtika.test.postgresql.dummyapp.TestApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

import static com.playtika.test.postgresql.PostgreSQLProperties.BEAN_NAME_EMBEDDED_POSTGRESQL;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

class AutoConfiguredDatasourceDependsOnTest {

    @ActiveProfiles("enabled")
    @SpringBootTest(classes = TestApplication.class)
    @DisplayName("Default AutoConfigured Datasource")
    @Nested
    class TestDefaults {

        @Autowired
        protected ConfigurableListableBeanFactory beanFactory;

        @Autowired
        protected JdbcTemplate jdbcTemplate;

        @Test
        void shouldConnectToPostgreSQL() {
            assertThat(jdbcTemplate.queryForObject("select version()", String.class)).contains("PostgreSQL");
        }

        @Test
        void shouldSetupDependsOnForAllDataSources() {
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
                    .contains(BEAN_NAME_EMBEDDED_POSTGRESQL);
        }
    }

    @TestPropertySource(properties = {
            "embedded.postgresql.docker-image=timescale/timescaledb:latest-pg11"
    })
    @Nested
    @DisplayName("AutoConfigured Datasource with timescaledb:latest-pg11")
    class Timescale12Image extends TestDefaults {
    }
}

