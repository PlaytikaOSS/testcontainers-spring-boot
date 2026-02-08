package com.playtika.testcontainer.clickhouse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;

import static com.playtika.testcontainer.clickhouse.ClickHouseProperties.BEAN_NAME_EMBEDDED_CLICK_HOUSE;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(classes = BaseEmbeddedClickHouseTest.TestConfiguration.class,
        properties = {
                "spring.profiles.active=enabled",
                "embedded.clickhouse.init-script-path=initScript.sql",
                "embedded.toxiproxy.proxies.clickhouse.enabled=true"
        })
public abstract class BaseEmbeddedClickHouseTest {

    @Autowired
    protected ConfigurableListableBeanFactory beanFactory;

    @Autowired
    protected ConfigurableEnvironment environment;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected void hasDependsOn(String beanName) {
        assertThat(beanFactory.getBeanDefinition(beanName).getDependsOn())
                .isNotNull()
                .isNotEmpty()
                .contains(BEAN_NAME_EMBEDDED_CLICK_HOUSE);
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}
