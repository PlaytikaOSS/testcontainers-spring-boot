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
package com.playtika.test.clickhouse;

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
import org.testcontainers.containers.ClickHouseContainer;
import org.testcontainers.shaded.com.google.common.base.Strings;
import org.testcontainers.utility.DockerImageName;

import java.util.LinkedHashMap;

import static com.playtika.test.clickhouse.ClickHouseProperties.BEAN_NAME_EMBEDDED_CLICK_HOUSE;
import static com.playtika.test.clickhouse.ClickHouseProperties.DEFAULT_DOCKER_IMAGE;
import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.clickhouse.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ClickHouseProperties.class)
public class EmbeddedClickHouseBootstrapConfiguration {

    @Bean(name = BEAN_NAME_EMBEDDED_CLICK_HOUSE, destroyMethod = "stop")
    public ConcreteClickHouseContainer clickHouseContainer(ConfigurableEnvironment environment,
                                                           ClickHouseProperties properties) {
        log.info("Starting ClickHouse server. Docker image: {}", properties.getDockerImage());

        DockerImageName dockerImageName = DockerImageName.parse(properties.dockerImage);

        if (properties.asCompatibleImage) {
            dockerImageName = dockerImageName.asCompatibleSubstituteFor(DEFAULT_DOCKER_IMAGE);
        }

        ConcreteClickHouseContainer clickHouseContainer = new ConcreteClickHouseContainer(dockerImageName);
        String username = Strings.isNullOrEmpty(properties.getUser()) ? clickHouseContainer.getUsername() : properties.getUser();
        String password = Strings.isNullOrEmpty(properties.getPassword()) ? clickHouseContainer.getPassword() : properties.getPassword();
        clickHouseContainer.addEnv("CLICKHOUSE_USER", username);
        clickHouseContainer.addEnv("CLICKHOUSE_PASSWORD", Strings.nullToEmpty(password));

        clickHouseContainer = (ConcreteClickHouseContainer) configureCommonsAndStart(clickHouseContainer, properties, log);

        registerClickHouseEnvironment(clickHouseContainer, environment, properties, username, password);

        return clickHouseContainer;
    }

    private void registerClickHouseEnvironment(ConcreteClickHouseContainer clickHouseContainer,
                                               ConfigurableEnvironment environment,
                                               ClickHouseProperties properties,
                                               String username, String password) {
        Integer mappedPort = clickHouseContainer.getMappedPort(properties.port);
        String host = clickHouseContainer.getContainerIpAddress();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.clickhouse.schema", "default");
        map.put("embedded.clickhouse.host", host);
        map.put("embedded.clickhouse.port", mappedPort);
        map.put("embedded.clickhouse.user", username);
        map.put("embedded.clickhouse.password", password);

        log.info("Started ClickHouse server. Connection details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedClickHouseInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

    private static class ConcreteClickHouseContainer extends ClickHouseContainer {
        public ConcreteClickHouseContainer(DockerImageName dockerImageName) {
            super(dockerImageName);
        }
    }
}
