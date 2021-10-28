package com.playtika.test.oracle;

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

import static com.playtika.test.oracle.OracleProperties.BEAN_NAME_EMBEDDED_ORACLE;

@Configuration
@AutoConfigureOrder
@ConditionalOnClass(DataSource.class)
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(name = "embedded.oracle.enabled", matchIfMissing = true)
@AutoConfigureAfter(name = "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration")
public class EmbeddedOracleDependenciesAutoConfiguration {

    @Bean
    public static BeanFactoryPostProcessor datasourceOracleDependencyPostProcessor() {
        return new DependsOnPostProcessor(DataSource.class, new String[]{BEAN_NAME_EMBEDDED_ORACLE});
    }
}
