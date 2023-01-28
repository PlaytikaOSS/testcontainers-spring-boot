package com.playtika.test.memsql;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import java.util.concurrent.Callable;

import static com.playtika.test.memsql.MemSqlProperties.BEAN_NAME_EMBEDDED_MEMSQL;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(classes = EmbeddedMemSqlBootstrapConfigurationTest.TestConfiguration.class,
        properties = {
                "spring.profiles.active=enabled",
                "embedded.memsql.install.enabled=true"
        })
public class EmbeddedMemSqlBootstrapConfigurationTest {

    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ConfigurableEnvironment environment;

//    @Autowired
//    NetworkTestOperations memsqlNetworkTestOperations;

    @Test
    public void shouldConnectToMemSQL() throws Exception {
        assertThat(jdbcTemplate.queryForObject("select @@version_comment", String.class)).contains("SingleStoreDB");
        jdbcTemplate.execute("create table foo (id int primary key);");
        jdbcTemplate.execute("insert into foo values (1), (2), (3);");
        assertThat(jdbcTemplate.queryForList("select * from foo")).hasSize(3);
    }

//    @Test
//    @Disabled("image doesn't support to simply install tc")
//    public void shouldEmulateLatency() throws Exception {
//        jdbcTemplate.execute("create table bar (id int primary key);");
//        jdbcTemplate.execute("insert into bar values (1), (2), (3);");
//
//        memsqlNetworkTestOperations.withNetworkLatency(ofMillis(1000),
//                () -> assertThat(durationOf(() -> jdbcTemplate.queryForList("select * from bar")))
//                        .isGreaterThan(1000L)
//        );
//
//        assertThat(durationOf(() -> jdbcTemplate.queryForList("select * from bar")))
//                .isLessThan(100L);
//    }

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
                .contains(BEAN_NAME_EMBEDDED_MEMSQL);
    }

    private static long durationOf(Callable<?> op) throws Exception {
        long start = System.currentTimeMillis();
        op.call();
        return System.currentTimeMillis() - start;
    }

    @Test
    public void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.memsql.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.memsql.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.memsql.schema")).isNotEmpty();
        assertThat(environment.getProperty("embedded.memsql.user")).isNotEmpty();
        assertThat(environment.getProperty("embedded.memsql.password")).isEqualTo("pass");
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}
