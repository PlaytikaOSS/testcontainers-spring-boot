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
package com.playtika.test.minio;

import com.github.dockerjava.api.model.Capability;
import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;

import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.containerLogsConsumer;
import static com.playtika.test.common.utils.ContainerUtils.startAndLogTime;
import static com.playtika.test.minio.MinioProperties.MINIO_BEAN_NAME;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(value = "embedded.minio.enabled", matchIfMissing = true)
public class EmbeddedMinioBootstrapConfiguration {

    @Bean
    @ConditionalOnMissingBean
    MinioProperties minioProperties() {
        return new MinioProperties();
    }

    @Bean(name = "minioWaitStrategy")
    @ConditionalOnMissingBean
    public MinioWaitStrategy minioWaitStrategy(MinioProperties properties) {
        return new DefaultMinioWaitStrategy(properties);
    }

    @Bean(name = MINIO_BEAN_NAME, destroyMethod = "stop")
    public GenericContainer minio(MinioWaitStrategy minioWaitStrategy,
                                  ConfigurableEnvironment environment,
                                  MinioProperties properties) {
        log.info("Starting Minio server. Docker image: {}", properties.dockerImage);

        GenericContainer minio =
                new GenericContainer<>(properties.dockerImage)
                        .withExposedPorts(properties.port)
                        .withLogConsumer(containerLogsConsumer(log))
                        .withEnv("MINIO_ACCESS_KEY", properties.accessKey)
                        .withEnv("MINIO_SECRET_KEY", properties.secretKey)
                        .withEnv("MINIO_USERNAME", properties.userName)
                        .withEnv("MINIO_GROUPNAME", properties.groupName)
                        .withEnv("MINIO_REGION", properties.region)
                        .withEnv("MINIO_WORM", properties.worm)
                        .withEnv("MINIO_BROWSER", properties.browser)
                        .withCommand("server", properties.directory)
                        .withCreateContainerCmdModifier(cmd -> cmd.withCapAdd(Capability.NET_ADMIN))
                        .waitingFor(minioWaitStrategy)
                        .withStartupTimeout(properties.getTimeoutDuration())
                        .withReuse(properties.isReuseContainer());

        startAndLogTime(minio);
        registerEnvironment(minio, environment, properties);
        return minio;
    }

    private void registerEnvironment(GenericContainer container,
                                     ConfigurableEnvironment environment,
                                     MinioProperties properties) {
        Integer mappedPort = container.getMappedPort(properties.port);
        String host = container.getContainerIpAddress();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.minio.host", host);
        map.put("embedded.minio.port", mappedPort);
        map.put("embedded.minio.accessKey", properties.accessKey);
        map.put("embedded.minio.secretKey", properties.secretKey);
        map.put("embedded.minio.region", properties.region);

        log.info("Started Minio server. Connection details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedMinioInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

}
