package com.playtika.test.memsql;

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

import static com.playtika.test.memsql.MemSqlProperties.BEAN_NAME_EMBEDDED_MEMSQL;

@AutoConfiguration(afterName = "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration")
@AutoConfigureOrder
@ConditionalOnClass(DataSource.class)
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(name = "embedded.memsql.enabled", matchIfMissing = true)
public class EmbeddedMemSqlDependenciesAutoConfiguration {

    @Configuration
    public static class EmbeddedMemSqlSourceDependencyContext {
        @Bean
        public static BeanFactoryPostProcessor datasourceMemsqlDependencyPostProcessor() {
            return new DependsOnPostProcessor(DataSource.class, new String[]{BEAN_NAME_EMBEDDED_MEMSQL});
        }
    }
}
