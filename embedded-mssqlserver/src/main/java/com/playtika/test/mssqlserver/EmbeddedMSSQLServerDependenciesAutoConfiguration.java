package com.playtika.test.mssqlserver;

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
@ConditionalOnProperty(name = "embedded.mssqlserver.enabled", matchIfMissing = true)
public class EmbeddedMSSQLServerDependenciesAutoConfiguration {

    @Bean
    public static BeanFactoryPostProcessor datasourceMSSQLServerDependencyPostProcessor() {
        return new DependsOnPostProcessor(DataSource.class, new String[]{MSSQLServerProperties.BEAN_NAME_EMBEDDED_MSSQLSERVER});
    }
}
