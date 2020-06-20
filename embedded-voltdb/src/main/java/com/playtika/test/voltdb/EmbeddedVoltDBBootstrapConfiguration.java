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
package com.playtika.test.voltdb;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;

import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.containerLogsConsumer;
import static com.playtika.test.voltdb.VoltDBProperties.BEAN_NAME_EMBEDDED_VOLTDB;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.voltdb.enabled", matchIfMissing = true)
@EnableConfigurationProperties(VoltDBProperties.class)
public class EmbeddedVoltDBBootstrapConfiguration {

    @Bean
    @ConditionalOnMissingBean
    VoltDBStatusCheck voltDBStartupCheckStrategy() {
        return new VoltDBStatusCheck();
    }

    @Bean(name = BEAN_NAME_EMBEDDED_VOLTDB, destroyMethod = "stop")
    public GenericContainer voltDB(ConfigurableEnvironment environment,
                                   VoltDBProperties properties,
                                   VoltDBStatusCheck voltDbStatusCheck) {
        log.info("Starting VoltDB server. Docker image: {}", properties.dockerImage);

        GenericContainer voltDB =
                new GenericContainer(properties.dockerImage)
                        .withEnv("HOST_COUNT", "1")
                        .withLogConsumer(containerLogsConsumer(log))
                        .withExposedPorts(properties.port)
                        .waitingFor(voltDbStatusCheck)
                        .withStartupTimeout(properties.getTimeoutDuration())
                        .withReuse(properties.isReuseContainer());
        voltDB.start();
        registerVoltDBEnvironment(voltDB, environment, properties);
        return voltDB;
    }

    private void registerVoltDBEnvironment(GenericContainer voltDB,
                                           ConfigurableEnvironment environment,
                                           VoltDBProperties properties) {
        Integer mappedPort = voltDB.getMappedPort(properties.port);
        String host = voltDB.getContainerIpAddress();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();

        map.put("embedded.voltdb.port", mappedPort);
        map.put("embedded.voltdb.host", host);

        String jdbcURL = "jdbc:voltdb://{}:{}";
        log.info("Started VoltDB server. Connection details: {}, " +
                "JDBC connection url: " + jdbcURL, map, host, mappedPort);

        MapPropertySource propertySource = new MapPropertySource("embeddedVoltDBInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
