package com.playtika.testcontainer.postgresql;

import com.playtika.testcontainer.postgresql.dummyapp.TestApplication;
import com.playtika.testcontainer.toxiproxy.ToxiproxyClientProxy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static com.playtika.testcontainer.postgresql.PostgreSQLProperties.BEAN_NAME_EMBEDDED_POSTGRESQL;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mirrors the key assertions of {@link EmbeddedPostgreSQLBootstrapConfigurationTest}, but with
 * {@code embedded.containers.parallelStartup} enabled, to prove the deferred/scheduled startup
 * path behaves identically to the default sequential one - including for the Toxiproxy proxy,
 * which reads the container's configured network alias rather than requiring it to be started.
 */
@ActiveProfiles("enabled")
@SpringBootTest(
        classes = TestApplication.class,
        properties = {
                "embedded.containers.parallelStartup=true",
                "embedded.toxiproxy.proxies.postgresql.enabled=true"
        }
)
class EmbeddedPostgreSQLParallelStartupTest {

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    ToxiproxyClientProxy postgresqlContainerProxy;

    @Test
    void shouldConnectToPostgreSQL() {
        assertThat(jdbcTemplate.queryForObject("select version()", String.class)).contains("PostgreSQL");
    }

    @Test
    void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.postgresql.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.postgresql.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.postgresql.schema")).isNotEmpty();
        assertThat(environment.getProperty("embedded.postgresql.user")).isNotEmpty();
        assertThat(environment.getProperty("embedded.postgresql.password")).isNotEmpty();
    }

    @Test
    void shouldSetupDependsOnForDataSource() {
        String[] beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, DataSource.class);
        assertThat(beanNamesForType).isNotEmpty();
        for (String beanName : beanNamesForType) {
            assertThat(beanFactory.getBeanDefinition(beanName).getDependsOn())
                    .isNotNull()
                    .isNotEmpty()
                    .contains(BEAN_NAME_EMBEDDED_POSTGRESQL);
        }
    }
}
