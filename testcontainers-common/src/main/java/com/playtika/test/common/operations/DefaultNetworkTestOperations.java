package com.playtika.test.common.operations;

import com.playtika.test.common.utils.ContainerUtils;
import com.playtika.test.common.utils.ThrowingRunnable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;

import java.time.Duration;
import java.util.concurrent.Callable;

@Slf4j
@RequiredArgsConstructor
public class DefaultNetworkTestOperations implements NetworkTestOperations {
    private final GenericContainer<?> targetContainer;

    @Override
    public void addNetworkLatencyForResponses(Duration delay) {
        ContainerUtils.executeAndCheckExitCode(targetContainer,
                "tc", "qdisc", "add", "dev", "eth0", "root", "netem", "delay", delay.toMillis() + "ms");
    }

    @Override
    public void removeNetworkLatencyForResponses() {
        ContainerUtils.executeAndCheckExitCode(targetContainer,
                "tc", "qdisc", "del", "dev", "eth0", "root");
    }

    @Override
    public void withNetworkLatency(Duration delay, ThrowingRunnable runnable) {
        try {
            addNetworkLatencyForResponses(delay);
            runnable.run();
        } catch (Exception e) {
            log.error("Failed trying to add network latency", e);
            throw new RuntimeException(e);
        } finally {
            removeNetworkLatencyForResponses();
        }
    }

    @Override
    public <T> T withNetworkLatency(Duration delay, Callable<T> callable) {
        try {
            addNetworkLatencyForResponses(delay);
            return callable.call();
        } catch (Exception e) {
            log.error("Failed trying to add network latency", e);
            throw new RuntimeException(e);
        } finally {
            removeNetworkLatencyForResponses();
        }
    }

}
