package com.playtika.test.db2;

import com.playtika.test.common.spring.DependsOnPostProcessor;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

@AutoConfiguration(afterName = "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration")
@ConditionalOnClass(DataSource.class)
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(name = "embedded.db2.enabled", matchIfMissing = true)
public class EmbeddedDb2DependenciesAutoConfiguration {

    @Bean
    public static BeanFactoryPostProcessor datasourceDb2DependencyPostProcessor() {
        return new DependsOnPostProcessor(DataSource.class, new String[]{Db2Properties.BEAN_NAME_EMBEDDED_DB2});
    }
}
