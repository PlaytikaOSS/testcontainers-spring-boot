package com.playtika.test.mariadb;

import com.playtika.test.common.spring.DependsOnPostProcessor;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

import static com.playtika.test.mariadb.MariaDBProperties.BEAN_NAME_EMBEDDED_MARIADB;

@AutoConfiguration(afterName = "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration")
@AutoConfigureOrder
@ConditionalOnClass(DataSource.class)
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(name = "embedded.mariadb.enabled", matchIfMissing = true)
public class EmbeddedMariaDBDependenciesAutoConfiguration {

    @Configuration
    public static class EmbeddedMariaDbDataSourceDependencyContext {
        @Bean
        public static BeanFactoryPostProcessor datasourceMariaDbDependencyPostProcessor() {
            return new DependsOnPostProcessor(DataSource.class, new String[]{BEAN_NAME_EMBEDDED_MARIADB});
        }
    }
}
