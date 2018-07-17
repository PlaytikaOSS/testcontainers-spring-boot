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
package com.playtika.test.couchbase;

import com.playtika.test.couchbase.rest.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.WaitAllStrategy;
import org.testcontainers.containers.wait.WaitStrategy;

import static com.playtika.test.common.utils.ContainerUtils.DEFAULT_CONTAINER_WAIT_DURATION;
import static com.playtika.test.common.utils.ContainerUtils.containerLogsConsumer;

/**
 * https://blog.couchbase.com/testing-spring-data-couchbase-applications-with-testcontainers/
 */
@Slf4j
@AllArgsConstructor
class CouchbaseContainerFactory {

    public static final String COUCHBASE_HOST_NAME = "couchbase.testcontainer.docker";

    protected static GenericContainer create(CouchbaseProperties properties, Logger containerLogger, Network network) {
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
                .withLogConsumer(containerLogsConsumer(containerLogger))
                .withNetwork(network)
                .withNetworkAliases(COUCHBASE_HOST_NAME)
                .waitingFor(getCompositeWaitStrategy(properties));
    }

    /**
     * https://developer.couchbase.com/documentation/server/current/rest-api/rest-node-provisioning.html
     */
    private static WaitStrategy getCompositeWaitStrategy(CouchbaseProperties properties) {
        return new WaitAllStrategy()
                .withStrategy(new SetupNodeStorage(properties))
                .withStrategy(new SetupRamQuotas(properties))
                .withStrategy(new SetupServices(properties))
                .withStrategy(new SetupIndexesType(properties))
                .withStrategy(new SetupAdminUserAndPassword(properties))
                .withStrategy(new CreateBucket(properties))
                .withStrategy(new CreatePrimaryIndex(properties))
                .withStrategy(new CreateBucketUser(properties))
                .withStartupTimeout(DEFAULT_CONTAINER_WAIT_DURATION);
    }
}
