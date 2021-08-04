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
package com.playtika.test.common.spring;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.util.List;

import com.playtika.test.common.properties.TestcontainersProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.testcontainers.containers.GenericContainer;

//TODO: Drop this workaround after proper fix available https://github.com/spring-cloud/spring-cloud-commons/issues/752

@Slf4j
@Configuration
@AutoConfigureOrder(value = Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "embedded.containers", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(TestcontainersProperties.class)
public class EmbeddedContainersShutdownAutoConfiguration {

    public static final String ALL_CONTAINERS = "allContainers";

    @Bean(name = ALL_CONTAINERS)
    public AllContainers allContainers(@Autowired(required = false) DockerPresenceMarker dockerAvailable,
                                       @Autowired(required = false) GenericContainer[] allContainers,
                                       TestcontainersProperties testcontainersProperties) {
        //Docker presence marker is not available == no spring cloud
        if (dockerAvailable == null)
            throw new NoDockerPresenceMarkerException("No docker presence marker available. " +
                    "Did you add spring cloud starter into classpath?");

        List<GenericContainer> containers = allContainers != null ? asList(allContainers) : emptyList();
        return new AllContainers(containers, testcontainersProperties);
    }
}
