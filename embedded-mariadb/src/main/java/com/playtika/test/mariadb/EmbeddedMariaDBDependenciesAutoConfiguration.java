package com.playtika.test.mariadb;

import com.playtika.test.common.spring.DependsOnPostProcessor;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

import static com.playtika.test.mariadb.MariaDBProperties.BEAN_NAME_EMBEDDED_MARIADB;

@Configuration
@AutoConfigureOrder
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
@ConditionalOnProperty(name = "embedded.mariadb.enabled", matchIfMissing = true)
@EnableConfigurationProperties(MariaDBProperties.class)
public class EmbeddedMariaDBDependenciesAutoConfiguration {


    @Configuration
    @ConditionalOnBean(DataSource.class)
    public static class EmbeddedMariaDbDataSourceDependencyContext {

        @Bean
        public BeanFactoryPostProcessor datasourceDependencyPostProcessor() {
            return new DependsOnPostProcessor(DataSource.class, new String[]{BEAN_NAME_EMBEDDED_MARIADB});
        }
    }
}
