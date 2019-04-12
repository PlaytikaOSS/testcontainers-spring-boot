/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Playtika
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

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.containerLogsConsumer;
import static com.playtika.test.postgresql.PostgreSQLProperties.BEAN_NAME_EMBEDDED_POSTGRESQL;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@Slf4j
@Configuration
@Order(HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "embedded.postgresql.enabled", matchIfMissing = true)
@EnableConfigurationProperties({PostgreSQLProperties.class, FilesToCopyProperties.class})
public class EmbeddedPostgreSQLBootstrapConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PostgreSQLStatusCheck postgresSQLStartupCheckStrategy(PostgreSQLProperties properties) {
        return new PostgreSQLStatusCheck(properties);
    }

    @Bean(name = BEAN_NAME_EMBEDDED_POSTGRESQL, destroyMethod = "stop")
    public GenericContainer postgresql(ConfigurableEnvironment environment,
                                       PostgreSQLProperties properties,
                                       FilesToCopyProperties filesToCopyProperties,
                                       PostgreSQLStatusCheck postgreSQLStatusCheck) {
        log.info("Starting postgresql server. Docker image: {}", properties.dockerImage);

        GenericContainer postgresql =
                new GenericContainer(properties.dockerImage)
                        .withEnv("POSTGRES_USER", properties.getUser())
                        .withEnv("POSTGRES_PASSWORD", properties.getPassword())
                        .withEnv("PGPASSWORD", properties.password) // for health check
                        .withEnv("POSTGRES_DB", properties.getDatabase())
                        .withCommand("postgres")
                        .withLogConsumer(containerLogsConsumer(log))
                        .withExposedPorts(properties.port)
                        .waitingFor(postgreSQLStatusCheck)
                        .withStartupTimeout(properties.getTimeoutDuration());
        postgresql.start();
        registerPostgresqlEnvironment(postgresql, environment, properties);
        copyFilesToContainer(postgresql, filesToCopyProperties);
        return postgresql;
    }

    private void registerPostgresqlEnvironment(GenericContainer postgresql,
                                               ConfigurableEnvironment environment,
                                               PostgreSQLProperties properties) {
        Integer mappedPort = postgresql.getMappedPort(properties.port);
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

    /**
     * Copies configured files from classpath or system path from properties
     * 
     * @param postgresql The postgres container
     * @param filesToCopy Configurations to copy files from classpath or system
     */
    private void copyFilesToContainer(GenericContainer postgresql, FilesToCopyProperties filesToCopy) {
        filesToCopy.getFilestocopy().stream().forEach(file -> {
            postgresql.copyFileToContainer(file.getInputResource().contains("classpath:")
            ? MountableFile.forClasspathResource(file.getInputResource().replace("classpath:", ""))
            : MountableFile.forHostPath(file.getInputResource()), file.getContainerPath());
        });
    }
}
