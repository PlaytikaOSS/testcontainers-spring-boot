package com.playtika.test.mariadb;

import com.playtika.test.common.spring.DependsOnPostProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;

import javax.sql.DataSource;
import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.containerLogsConsumer;
import static com.playtika.test.mariadb.MariaDBProperties.BEAN_NAME_EMBEDDED_MARIADB;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@Slf4j
@Configuration
@Order(HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "embedded.mariadb.enabled", matchIfMissing = true)
@EnableConfigurationProperties(MariaDBProperties.class)
public class EmbeddedMariaDBAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    MariaDBStartupCheckStrategy mariaDBStartupCheckStrategy(MariaDBProperties properties){
        return new MariaDBStartupCheckStrategy();
    }

    @Bean(name = BEAN_NAME_EMBEDDED_MARIADB, destroyMethod = "stop")
    public GenericContainer mariadb(ConfigurableEnvironment environment,
                                    MariaDBProperties properties,
                                    MariaDBStartupCheckStrategy mariaDBStartupCheckStrategy) throws Exception {
        GenericContainer mariadb =
                new GenericContainer(properties.dockerImage)
                        .withStartupCheckStrategy(mariaDBStartupCheckStrategy)
                        .withEnv("MYSQL_ALLOW_EMPTY_PASSWORD", "true")
                        .withEnv("MYSQL_USER", properties.getUser())
                        .withEnv("MYSQL_PASSWORD", properties.getPassword())
                        .withEnv("MYSQL_DATABASE", properties.getDatabase())
                        .withLogConsumer(containerLogsConsumer(log))
                        .withExposedPorts(properties.port);
        mariadb.start();
        registerMariadbEnvironment(mariadb, environment, properties);
        return mariadb;
    }

    private void registerMariadbEnvironment(GenericContainer mariadb,
                                            ConfigurableEnvironment environment,
                                            MariaDBProperties properties) {
        Integer mappedPort = mariadb.getMappedPort(properties.port);
        String host = mariadb.getContainerIpAddress();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.mariadb.port", mappedPort);
        map.put("embedded.mariadb.host", host);
        map.put("embedded.mariadb.schema", properties.getDatabase());
        map.put("embedded.mariadb.user", properties.getUser());
        map.put("embedded.mariadb.password", properties.getPassword());
        map.put("embedded.mariadb.pass", properties.getPassword());
        MapPropertySource propertySource = new MapPropertySource("embeddedMariaInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }


    @Configuration
    @ConditionalOnBean(DataSource.class)
    public static class EmbeddedMariaDbDataSourceDependencyContext {

        @Bean
        public BeanFactoryPostProcessor datasourceDependencyPostProcessor() {
            return new DependsOnPostProcessor(DataSource.class, new String[]{BEAN_NAME_EMBEDDED_MARIADB});
        }
    }
}
