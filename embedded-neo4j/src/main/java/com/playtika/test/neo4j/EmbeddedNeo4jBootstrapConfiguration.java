package com.playtika.test.neo4j;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.test.common.utils.ContainerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.Neo4jContainer;

import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.test.neo4j.Neo4jProperties.BEAN_NAME_EMBEDDED_NEO4J;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.neo4j.enabled", matchIfMissing = true)
@EnableConfigurationProperties(Neo4jProperties.class)
public class EmbeddedNeo4jBootstrapConfiguration {


    @Bean(name = BEAN_NAME_EMBEDDED_NEO4J, destroyMethod = "stop")
    public Neo4jContainer neo4j(ConfigurableEnvironment environment,
                                Neo4jProperties properties) {
        Neo4jContainer neo4j = new Neo4jContainer<>(ContainerUtils.getDockerImageName(properties))
                .withAdminPassword(properties.password);
        neo4j = (Neo4jContainer) configureCommonsAndStart(neo4j, properties, log);
        registerNeo4jEnvironment(neo4j, environment, properties);
        return neo4j;
    }

    private void registerNeo4jEnvironment(Neo4jContainer neo4j,
                                          ConfigurableEnvironment environment,
                                          Neo4jProperties properties) {
        Integer httpsPort = neo4j.getMappedPort(properties.httpsPort);
        Integer httpPort = neo4j.getMappedPort(properties.httpPort);
        Integer boltPort = neo4j.getMappedPort(properties.boltPort);
        String host = neo4j.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.neo4j.httpsPort", httpsPort);
        map.put("embedded.neo4j.httpPort", httpPort);
        map.put("embedded.neo4j.boltPort", boltPort);
        map.put("embedded.neo4j.host", host);
        map.put("embedded.neo4j.password", properties.getPassword());
        map.put("embedded.neo4j.user", properties.getUser());

        log.info("Started neo4j server. Connection details {},  " +
                        "Admin UI: http://localhost:{}, user: {}, password: {}",
                map, httpPort, properties.getUser(), properties.getPassword());

        MapPropertySource propertySource = new MapPropertySource("embeddedNeo4jInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
