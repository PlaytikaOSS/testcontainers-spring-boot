/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Playtika
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
package com.playtika.test.cassandra;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ResourceLoader;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.GenericContainer;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.playtika.test.cassandra.CassandraProperties.BEAN_NAME_EMBEDDED_CASSANDRA;
import static com.playtika.test.cassandra.CassandraProperties.DEFAULT_DATACENTER;
import static com.playtika.test.common.utils.FileUtils.resolveTemplate;
import static com.playtika.test.common.utils.ContainerUtils.containerLogsConsumer;
import static com.playtika.test.common.utils.ContainerUtils.startAndLogTime;

@Slf4j
@Configuration
@AutoConfigureOrder
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(name = "embedded.cassandra.enabled", matchIfMissing = true)
@EnableConfigurationProperties(CassandraProperties.class)
@RequiredArgsConstructor
public class EmbeddedCassandraBootstrapConfiguration {

    private final ResourceLoader resourceLoader;

    @Bean(name = BEAN_NAME_EMBEDDED_CASSANDRA, destroyMethod = "stop")
    public CassandraContainer cassandra(ConfigurableEnvironment environment,
                                  CassandraProperties properties) throws Exception {

        log.info("Starting Cassandra cluster. Docker image: {}", properties.dockerImage);

        prepareCassandraInitScript(properties);

        CassandraContainer cassandra = new CassandraContainer<>(properties.dockerImage)
                .withReuse(properties.isReuseContainer())
                .withInitScript("cassandra-init.sql")
                .withLogConsumer(containerLogsConsumer(log))
                .withExposedPorts(properties.getPort());
        startAndLogTime(cassandra);
        Map<String, Object> cassandraEnv = registerCassandraEnvironment(environment, cassandra, properties);

        log.info("Started Cassandra. Connection details: {}", cassandraEnv);
        return cassandra;
    }

    static Map<String, Object> registerCassandraEnvironment(ConfigurableEnvironment environment,
                                                            CassandraContainer cassandra,
                                                            CassandraProperties properties) {
        String host = cassandra.getContainerIpAddress();
        Integer mappedPort = cassandra.getMappedPort(properties.getPort());
        LinkedHashMap<String, Object> cassandraEnv = new LinkedHashMap<>();
        cassandraEnv.put("embedded.cassandra.port", mappedPort);
        cassandraEnv.put("embedded.cassandra.host", host);
        cassandraEnv.put("embedded.cassandra.datacenter", DEFAULT_DATACENTER);
        cassandraEnv.put("embedded.cassandra.keyspace-name", properties.keyspaceName);
        MapPropertySource propertySource = new MapPropertySource("embeddedCassandraInfo", cassandraEnv);
        environment.getPropertySources().addFirst(propertySource);
        return cassandraEnv;
    }

    private void prepareCassandraInitScript(CassandraProperties properties) throws Exception {
        resolveTemplate(resourceLoader, "cassandra-init.sql", content -> content
                .replace("{{keyspaceName}}", properties.keyspaceName));
    }
}
