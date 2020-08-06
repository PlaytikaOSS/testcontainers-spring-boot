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
package com.playtika.test.oracle;

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
import org.testcontainers.containers.OracleContainer;

import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.containerLogsConsumer;
import static com.playtika.test.oracle.OracleProperties.BEAN_NAME_EMBEDDED_ORACLE;
import static com.playtika.test.oracle.OracleProperties.ORACLE_DB;
import static com.playtika.test.oracle.OracleProperties.ORACLE_PORT;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.oracle.enabled", matchIfMissing = true)
@EnableConfigurationProperties(OracleProperties.class)
public class EmbeddedOracleBootstrapConfiguration {

    @Bean(name = BEAN_NAME_EMBEDDED_ORACLE, destroyMethod = "stop")
    public OracleContainer oracle(ConfigurableEnvironment environment,
                                  OracleProperties properties) {
        log.info("Starting oracle server. Docker image: {}", properties.dockerImage);

        OracleContainer oracle =
                new OracleContainer(properties.dockerImage)
                        .withUsername(properties.getUser())
                        .withPassword(properties.getPassword())
                        .withLogConsumer(containerLogsConsumer(log))
                        .withStartupTimeout(properties.getTimeoutDuration())
                        .withInitScript(properties.initScriptPath)
                        .withReuse(properties.isReuseContainer());
        oracle.start();
        registerOracleEnvironment(oracle, environment, properties);
        return oracle;
    }

    private void registerOracleEnvironment(OracleContainer oracle,
                                           ConfigurableEnvironment environment,
                                           OracleProperties properties) {
        Integer mappedPort = oracle.getMappedPort(ORACLE_PORT);
        String host = oracle.getContainerIpAddress();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.oracle.port", mappedPort);
        map.put("embedded.oracle.host", host);
        map.put("embedded.oracle.database", properties.getDatabase());
        map.put("embedded.oracle.user", properties.getUser());
        map.put("embedded.oracle.password", properties.getPassword());

        String jdbcURL = "jdbc:oracle://{}:{}/{}";
        log.info("Started oracle server. Connection details: {}, " +
                "JDBC connection url: " + jdbcURL, map, host, mappedPort, ORACLE_DB);

        MapPropertySource propertySource = new MapPropertySource("embeddedOracleInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
