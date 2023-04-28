package com.playtika.testcontainer.oracle;

import com.playtika.testcontainer.oracle.dummy.TestApplication;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static com.playtika.testcontainer.oracle.OracleProperties.BEAN_NAME_EMBEDDED_ORACLE;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("enabled")
@SpringBootTest(classes = {TestApplication.class,
        EmbeddedOracleBootstrapConfigurationTest.TestConfiguration.class},
        properties = "embedded.oracle.init-script-path=initScript.sql"
)
class EmbeddedOracleBootstrapConfigurationTest {

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ConfigurableEnvironment environment;


    @Test
    void shouldConnectToOracle() {
        assertThat(jdbcTemplate.queryForObject("select 1 from dual", String.class)).contains("1");
    }

    @Test
    void shouldSaveAndGetUnicode() {
        jdbcTemplate.execute("CREATE TABLE employee(id NUMBER, name VARCHAR2(64 CHAR))");
        jdbcTemplate.execute("insert into employee (id, name) values (1, 'some data \uD83D\uDE22')");

        assertThat(jdbcTemplate.queryForObject("select name from employee where id = 1", String.class)).isEqualTo("some data \uD83D\uDE22");
    }

    @Test
    public void shouldInitDBForOracle() throws Exception {
        assertThat(jdbcTemplate.queryForObject("select count(first_name) from users where first_name = 'Sam' ", Integer.class)).isEqualTo(1);
    }

    @Test
    void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.oracle.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.oracle.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.oracle.database")).isNotEmpty();
        assertThat(environment.getProperty("embedded.oracle.user")).isNotEmpty();
        assertThat(environment.getProperty("embedded.oracle.password")).isNotEmpty();
        assertThat(environment.getProperty("embedded.oracle.init-script-path")).isNotEmpty();
    }

    @Test
    void shouldSetupDependsOnForAllDataSources() {
        String[] beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, DataSource.class);
        assertThat(beanNamesForType)
                .as("Custom datasource should be present")
                .hasSize(1)
                .contains("customDatasource");
        asList(beanNamesForType).forEach(this::hasDependsOn);
    }

    @Test
    void shouldConnectDB() {
        assertThat(jdbcTemplate.queryForObject("select 1 from dual", String.class)).contains("1");
    }

    private void hasDependsOn(String beanName) {
        assertThat(beanFactory.getBeanDefinition(beanName).getDependsOn())
                .isNotNull()
                .isNotEmpty()
                .contains(BEAN_NAME_EMBEDDED_ORACLE);
    }

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
            poolConfiguration.setDriverClassName("oracle.jdbc.driver.OracleDriver");
            poolConfiguration.setUsername(user);
            poolConfiguration.setPassword(password);
            poolConfiguration.setTestOnBorrow(true);
            poolConfiguration.setTestOnReturn(true);
            return new org.apache.tomcat.jdbc.pool.DataSource(poolConfiguration);
        }
    }
}
