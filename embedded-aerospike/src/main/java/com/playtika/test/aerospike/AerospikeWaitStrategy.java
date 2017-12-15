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
package com.playtika.test.aerospike;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.AerospikeException;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.NetworkSettings;
import com.github.dockerjava.api.model.Ports;
import com.playtika.test.common.checks.AbstractRetryingWaitStrategy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.DockerClientFactory;

import java.util.Map;

@Slf4j
@AllArgsConstructor
public class AerospikeWaitStrategy extends AbstractRetryingWaitStrategy {

    private final AerospikeProperties properties;

    @Override
    protected boolean isReady() {
        String containerId = container.getContainerId();
        log.debug("Check Aerospike container {} status", containerId);

        InspectContainerResponse containerInfo = container.getContainerInfo();
        if (containerInfo == null) {
            log.debug("Aerospike container[{}] doesn't contain info. Abnormal situation, should not happen.", containerId);
            return false;
        }

        int port = getMappedPort(containerInfo.getNetworkSettings(), properties.port);
        String host = DockerClientFactory.instance().dockerHostIpAddress();

        //TODO: Remove dependency to client https://www.aerospike.com/docs/tools/asmonitor/common_tasks.html
        try (AerospikeClient client = new AerospikeClient(host, port)) {
            return client.isConnected();
        } catch (AerospikeException.Connection e) {
            log.debug("Aerospike container: {} not yet started. {}", containerId, e.getMessage());
        }
        return false;
    }

    private int getMappedPort(NetworkSettings networkSettings, int originalPort) {
        ExposedPort exposedPort = new ExposedPort(originalPort);
        Ports ports = networkSettings.getPorts();
        Map<ExposedPort, Ports.Binding[]> bindings = ports.getBindings();
        Ports.Binding[] binding = bindings.get(exposedPort);
        return Integer.valueOf(binding[0].getHostPortSpec());
    }
}
