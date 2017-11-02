/*
* The MIT License (MIT)
*
* Copyright (c) 2017 Playtika
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
package com.playtika.test.memsql;

import com.playtika.test.common.spring.DependsOnPostProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;

import javax.sql.DataSource;
import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.containerLogsConsumer;
import static com.playtika.test.memsql.MemSqlProperties.BEAN_NAME_EMBEDDED_MEMSQL;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@Slf4j
@Configuration
@Order(HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "embedded.memsql.enabled", matchIfMissing = true)
@EnableConfigurationProperties(MemSqlProperties.class)
public class EmbeddedMemSqlAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    MemSqlStatusCheck memSqlStartupCheckStrategy(MemSqlProperties properties) {
        return new MemSqlStatusCheck();
    }

    @Bean(name = BEAN_NAME_EMBEDDED_MEMSQL, destroyMethod = "stop")
    public GenericContainer memsql(ConfigurableEnvironment environment,
                                   MemSqlProperties properties,
                                   MemSqlStatusCheck memSqlStatusCheck) throws Exception {
        log.info("Starting memsql server. Docker image: {}", properties.dockerImage);

        GenericContainer memsql =
                new GenericContainer(properties.dockerImage)
                        .withEnv("IGNORE_MIN_REQUIREMENTS", "1")
                        .withStartupCheckStrategy(memSqlStatusCheck)
                        .withLogConsumer(containerLogsConsumer(log))
                        .withExposedPorts(properties.port, properties.adminPort)
                        .withClasspathResourceMapping(
                                "mem.sql",
                                "/schema.sql",
                                BindMode.READ_ONLY);
        ;
        memsql.start();
        registerMemSqlEnvironment(memsql, environment, properties);
        return memsql;
    }

    private void registerMemSqlEnvironment(GenericContainer memsql,
                                           ConfigurableEnvironment environment,
                                           MemSqlProperties properties) {
        Integer mappedPort = memsql.getMappedPort(properties.port);
        Integer mappedAdminPort = memsql.getMappedPort(properties.adminPort);
        String host = memsql.getContainerIpAddress();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.memsql.adminPort", mappedAdminPort);
        map.put("embedded.memsql.port", mappedPort);
        map.put("embedded.memsql.host", host);
        map.put("embedded.memsql.schema", properties.getDatabase());
        map.put("embedded.memsql.user", properties.getUser());
        map.put("embedded.memsql.password", properties.getPassword());

        log.info("Started memsql server. Connection details {}. Admin UI: http://localhost:{}, user: {}, password: {}",
                map, mappedAdminPort, properties.getUser(), properties.getPassword());

        MapPropertySource propertySource = new MapPropertySource("embeddedMemSqlInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

    @Configuration
    @ConditionalOnBean(DataSource.class)
    public static class EmbeddedMemSqlDataSourceDependencyContext {

        @Bean
        public BeanFactoryPostProcessor datasourceDependencyPostProcessor() {
            return new DependsOnPostProcessor(DataSource.class, new String[]{BEAN_NAME_EMBEDDED_MEMSQL});
        }
    }
}
