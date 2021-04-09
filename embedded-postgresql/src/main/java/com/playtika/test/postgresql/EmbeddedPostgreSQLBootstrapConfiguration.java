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
package com.playtika.test.postgresql;

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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.test.postgresql.PostgreSQLProperties.BEAN_NAME_EMBEDDED_POSTGRESQL;
import static java.time.temporal.ChronoUnit.SECONDS;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.postgresql.enabled", matchIfMissing = true)
@EnableConfigurationProperties(PostgreSQLProperties.class)
public class EmbeddedPostgreSQLBootstrapConfiguration {

    @Bean(name = BEAN_NAME_EMBEDDED_POSTGRESQL, destroyMethod = "stop")
    public ConcretePostgreSQLContainer postgresql(ConfigurableEnvironment environment,
                                                  PostgreSQLProperties properties) {
        log.info("Starting postgresql server. Docker image: {}", properties.dockerImage);

        ConcretePostgreSQLContainer postgresql =
                new ConcretePostgreSQLContainer(DockerImageName.parse(properties.dockerImage)
                        .asCompatibleSubstituteFor("postgres"))
                        .withUsername(properties.getUser())
                        .withPassword(properties.getPassword())
                        .withDatabaseName(properties.getDatabase())
                        .withInitScript(properties.initScriptPath);

        WaitStrategy waitStrategy = new LogMessageWaitStrategy()
            .withRegEx(properties.getStartupLogCheckRegex())
            .withTimes(2)
            .withStartupTimeout(Duration.of(properties.getWaitTimeoutInSeconds(), SECONDS));
        postgresql.setWaitStrategy(waitStrategy);

        postgresql = (ConcretePostgreSQLContainer) configureCommonsAndStart(postgresql, properties, log);
        registerPostgresqlEnvironment(postgresql, environment, properties);
        return postgresql;
    }

    private void registerPostgresqlEnvironment(ConcretePostgreSQLContainer postgresql,
                                               ConfigurableEnvironment environment,
                                               PostgreSQLProperties properties) {
        Integer mappedPort = postgresql.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT);
        String host = postgresql.getContainerIpAddress();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.postgresql.port", mappedPort);
        map.put("embedded.postgresql.host", host);
        map.put("embedded.postgresql.schema", properties.getDatabase());
        map.put("embedded.postgresql.user", properties.getUser());
        map.put("embedded.postgresql.password", properties.getPassword());

        String jdbcURL = "jdbc:postgresql://{}:{}/{}";
        log.info("Started postgresql server. Connection details: {}, " +
                "JDBC connection url: " + jdbcURL, map, host, mappedPort, properties.getDatabase());

        MapPropertySource propertySource = new MapPropertySource("embeddedPostgreInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

    private static class ConcretePostgreSQLContainer extends PostgreSQLContainer<ConcretePostgreSQLContainer> {
        public ConcretePostgreSQLContainer(DockerImageName dockerImageName) {
            super(dockerImageName);
        }
    }
}
