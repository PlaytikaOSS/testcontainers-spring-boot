package com.playtika.test.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.playtika.test.common.spring.DependsOnPostProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.playtika.test.cassandra.CassandraProperties.BEAN_NAME_EMBEDDED_CASSANDRA;

@Slf4j
@AutoConfiguration(afterName = "org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration")
@ConditionalOnClass(CqlSession.class)
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(name = "embedded.cassandra.enabled", matchIfMissing = true)
public class EmbeddedCassandraDependenciesAutoConfiguration {

    @Configuration
    public static class CassandraSessionDependencyContextHelper {
        @Bean
        public static BeanFactoryPostProcessor cassandraSessionDependencyPostProcessor() {
            return new DependsOnPostProcessor(CqlSession.class, new String[]{BEAN_NAME_EMBEDDED_CASSANDRA});
        }
    }
}
