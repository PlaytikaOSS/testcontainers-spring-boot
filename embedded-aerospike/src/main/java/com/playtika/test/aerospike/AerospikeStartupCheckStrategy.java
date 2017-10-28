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
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.NetworkSettings;
import com.github.dockerjava.api.model.Ports;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.startupcheck.StartupCheckStrategy;

import java.util.Map;

@Slf4j
@AllArgsConstructor
public class AerospikeStartupCheckStrategy extends StartupCheckStrategy {

    AerospikeProperties properties;

    @Override
    public StartupStatus checkStartupState(DockerClient dockerClient, String containerId) {
        log.info("Check Aerospike container {} status", containerId);

        InspectContainerResponse response = dockerClient.inspectContainerCmd(containerId).exec();
        if (!response.getState().getRunning()) {
            return StartupStatus.FAILED;
        }

        int port = getMappedPort(response.getNetworkSettings(), properties.port);
        String host = DockerClientFactory.instance().dockerHostIpAddress();

        //TODO: Remove dependency to client https://www.aerospike.com/docs/tools/asmonitor/common_tasks.html
        try (AerospikeClient client = new AerospikeClient(host, port)) {
            if (client.isConnected()) {
                return StartupStatus.SUCCESSFUL;
            }
            return StartupStatus.NOT_YET_KNOWN;
        } catch (AerospikeException.Connection e) {
            log.warn("Aerospike container: {} not yet started. {}", containerId, e.getMessage());
        }

        return StartupStatus.NOT_YET_KNOWN;
    }

    private int getMappedPort(NetworkSettings networkSettings, int originalPort) {
        ExposedPort exposedPort = new ExposedPort(originalPort);
        Ports ports = networkSettings.getPorts();
        Map<ExposedPort, Ports.Binding[]> bindings = ports.getBindings();
        Ports.Binding[] binding = bindings.get(exposedPort);
        return Integer.valueOf(binding[0].getHostPortSpec());
    }
}
