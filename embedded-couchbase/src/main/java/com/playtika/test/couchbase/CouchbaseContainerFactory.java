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
package com.playtika.test.couchbase;

import com.playtika.test.common.checks.AbstractInitOnStartupStrategy;
import com.playtika.test.common.checks.CompositeStartupCheckStrategy;
import com.playtika.test.couchbase.rest.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.StartupCheckStrategy;

import java.util.List;

import static com.playtika.test.common.utils.ContainerUtils.containerLogsConsumer;
import static java.util.Arrays.asList;

/**
 * https://blog.couchbase.com/testing-spring-data-couchbase-applications-with-testcontainers/
 */
@Slf4j
@AllArgsConstructor
class CouchbaseContainerFactory {

    static GenericContainer create(CouchbaseProperties properties, Logger containerLogger) {
        return new FixedHostPortGenericContainer<>(properties.dockerImage)
                .withFixedExposedPort(properties.carrierDirectPort, properties.carrierDirectPort)
                .withFixedExposedPort(properties.httpDirectPort, properties.httpDirectPort)
                .withFixedExposedPort(properties.queryServicePort, properties.queryServicePort)
                .withFixedExposedPort(properties.queryRestTrafficPort, properties.queryRestTrafficPort)
                .withFixedExposedPort(properties.searchServicePort, properties.searchServicePort)
                .withFixedExposedPort(properties.analyticsServicePort, properties.analyticsServicePort)
                .withFixedExposedPort(properties.memcachedSslPort, properties.memcachedSslPort)
                .withFixedExposedPort(properties.memcachedPort, properties.memcachedPort)
                .withFixedExposedPort(properties.queryRestTrafficSslPort, properties.queryRestTrafficSslPort)
                .withFixedExposedPort(properties.queryServiceSslPort, properties.queryServiceSslPort)
                .withStartupCheckStrategy(getCompositeStartupCheckStrategy(properties))
                .withLogConsumer(containerLogsConsumer(containerLogger));
    }

    /**
     * https://developer.couchbase.com/documentation/server/current/rest-api/rest-node-provisioning.html
     */
    private static CompositeStartupCheckStrategy getCompositeStartupCheckStrategy(CouchbaseProperties properties) {
        List<StartupCheckStrategy> strategies = asList(new AbstractInitOnStartupStrategy[]{
                new SetupNodeStorage(properties),
                new SetupRamQuotas(properties),
                new SetupServices(properties),
                new SetupIndexesType(properties),
                new SetupAdminUserAndPassword(properties),
                new CreateBucket(properties),
                new CreatePrimaryIndex(properties),
                new CreateBucketUser(properties),
        });
        return CompositeStartupCheckStrategy.builder()
                .checksToPerform(strategies)
                .build();
    }
}
