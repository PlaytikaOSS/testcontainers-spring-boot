package com.playtika.test.neo4j;

import com.playtika.test.common.spring.DependsOnPostProcessor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.playtika.test.neo4j.Neo4jProperties.BEAN_NAME_EMBEDDED_NEO4J;

@Slf4j
@AutoConfiguration(afterName = "org.springframework.boot.autoconfigure.data.neo4j.Neo4jDataAutoConfiguration")
@AutoConfigureOrder
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(name = "embedded.neo4j.enabled", matchIfMissing = true)
public class EmbeddedNeo4jDependenciesAutoConfiguration {

    @Configuration
    @ConditionalOnClass(Session.class)
    public static class Neo4jSessionDependencyContext {
        @Bean
        public static BeanFactoryPostProcessor neo4jSessionDependencyPostProcessor() {
            return new DependsOnPostProcessor(Session.class, new String[]{BEAN_NAME_EMBEDDED_NEO4J});
        }
    }

    @Configuration
    @ConditionalOnClass(SessionFactory.class)
    public static class Neo4jSessionFactoryDependencyContext {
        @Bean
        public static BeanFactoryPostProcessor neo4jSessionFactoryDependencyPostProcessor() {
            return new DependsOnPostProcessor(SessionFactory.class, new String[]{BEAN_NAME_EMBEDDED_NEO4J});
        }
    }

    @Configuration
    @ConditionalOnClass(Driver.class)
    public static class Neo4jDriverDependencyContext {
        @Bean
        public static BeanFactoryPostProcessor neo4jDriverDependencyPostProcessor() {
            return new DependsOnPostProcessor(Driver.class, new String[]{BEAN_NAME_EMBEDDED_NEO4J});
        }
    }
}
