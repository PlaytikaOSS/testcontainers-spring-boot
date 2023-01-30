package com.playtika.test.clickhouse;

import com.playtika.test.common.spring.DependsOnPostProcessor;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@AutoConfigureOrder
@ConditionalOnClass(DataSource.class)
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(name = "embedded.clickhouse.enabled", havingValue = "true", matchIfMissing = true)
public class EmbeddedClickHouseDependenciesAutoConfiguration {

    @Configuration
    public static class ClickHouseDependencyContextHelper {
        @Bean
        public static BeanFactoryPostProcessor clickHouseContainerDependencyPostProcessor() {
            return new DependsOnPostProcessor(DataSource.class, new String[]{ClickHouseProperties.BEAN_NAME_EMBEDDED_CLICK_HOUSE});
        }
    }
}
