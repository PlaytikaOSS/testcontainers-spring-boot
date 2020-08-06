/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Playtika
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.playtika.test.neo4j;

import com.github.dockerjava.api.model.Capability;
import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
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

import static com.playtika.test.common.utils.ContainerUtils.containerLogsConsumer;
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
                                Neo4jProperties properties){

        log.info("Starting neo4j server. Docker image: {}", properties.dockerImage);

        Neo4jContainer neo4j = new Neo4jContainer<>(properties.dockerImage)
                .withAdminPassword(properties.password)
                .withLogConsumer(containerLogsConsumer(log))
                .withCreateContainerCmdModifier(cmd -> cmd.withCapAdd(Capability.NET_ADMIN))
                .withStartupTimeout(properties.getTimeoutDuration())
                .withReuse(properties.isReuseContainer());
        neo4j.start();
        registerNeo4jEnvironment(neo4j, environment, properties);
        return neo4j;
    }

    private void registerNeo4jEnvironment(Neo4jContainer neo4j,
                                          ConfigurableEnvironment environment,
                                          Neo4jProperties properties) {
        Integer httpsPort = neo4j.getMappedPort(properties.httpsPort);
        Integer httpPort = neo4j.getMappedPort(properties.httpPort);
        Integer boltPort = neo4j.getMappedPort(properties.boltPort);
        String host = neo4j.getContainerIpAddress();

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
