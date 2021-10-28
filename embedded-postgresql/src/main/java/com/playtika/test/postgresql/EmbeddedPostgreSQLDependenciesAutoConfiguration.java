package com.playtika.test.postgresql;

import com.playtika.test.common.spring.DependsOnPostProcessor;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

import static com.playtika.test.postgresql.PostgreSQLProperties.BEAN_NAME_EMBEDDED_POSTGRESQL;

@Configuration
@AutoConfigureOrder
@ConditionalOnClass(DataSource.class)
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(name = "embedded.postgresql.enabled", matchIfMissing = true)
@AutoConfigureAfter(name = "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration")
public class EmbeddedPostgreSQLDependenciesAutoConfiguration {

    @Bean
    public static BeanFactoryPostProcessor datasourcePostgreSqlDependencyPostProcessor() {
        return new DependsOnPostProcessor(DataSource.class, new String[]{BEAN_NAME_EMBEDDED_POSTGRESQL});
    }
}
