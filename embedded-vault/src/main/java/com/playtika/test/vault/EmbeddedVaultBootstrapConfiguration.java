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
package com.playtika.test.vault;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.vault.VaultContainer;

import java.util.Arrays;
import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.containerLogsConsumer;
import static com.playtika.test.common.utils.ContainerUtils.startAndLogTime;
import static com.playtika.test.vault.VaultProperties.BEAN_NAME_EMBEDDED_VAULT;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@Slf4j
@Order(HIGHEST_PRECEDENCE)
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.vault.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(VaultProperties.class)
public class EmbeddedVaultBootstrapConfiguration {

    @Bean(name = BEAN_NAME_EMBEDDED_VAULT, destroyMethod = "stop")
    public VaultContainer vault(ConfigurableEnvironment environment, VaultProperties properties) {

        log.info("Starting vault server. Docker image: {}", properties.getDockerImage());


        VaultContainer vault = new VaultContainer<>(properties.getDockerImage())
                .withVaultToken(properties.getToken())
                .withLogConsumer(containerLogsConsumer(log))
                .withExposedPorts(properties.getPort())
                .withStartupTimeout(properties.getTimeoutDuration())
                .withReuse(properties.isReuseContainer());

        String[] secrets = properties.getSecrets().entrySet().stream()
                .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
                .toArray(String[]::new);

        if (secrets.length > 0) {
            vault.withSecretInVault(properties.getPath(), secrets[0], Arrays.copyOfRange(secrets, 1, secrets.length));
        }

        startAndLogTime(vault);
        registerVaultEnvironment(vault, environment, properties);
        return vault;
    }

    private void registerVaultEnvironment(VaultContainer vault, ConfigurableEnvironment environment, VaultProperties properties) {
        Integer mappedPort = vault.getMappedPort(properties.getPort());
        String host = vault.getContainerIpAddress();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.vault.host", host);
        map.put("embedded.vault.port", mappedPort);
        map.put("embedded.vault.token", properties.getToken());

        log.info("Started vault. Connection Details: {}, Connection URI: http://{}:{}", map, host, mappedPort);

        MapPropertySource propertySource = new MapPropertySource("embeddedVaultInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
