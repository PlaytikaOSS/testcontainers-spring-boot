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
        String containerId = waitStrategyTarget.getContainerId();
        log.debug("Check Aerospike container {} status", containerId);

        InspectContainerResponse containerInfo = waitStrategyTarget.getContainerInfo();
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
