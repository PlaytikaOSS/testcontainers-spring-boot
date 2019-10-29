package com.playtika.test.common.operations;

import com.playtika.test.common.utils.ThrowingRunnable;
import lombok.RequiredArgsConstructor;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.Function;

@RequiredArgsConstructor
public class DefaultNetworkTestOperations implements NetworkTestOperations {
    private final GenericContainer targetContainer;

    @Override
    public void addNetworkLatencyForResponses(Duration delay) {
        executeCommandInContainer(container -> {
                    try {
                        return container.execInContainer("tc", "qdisc", "add", "dev", "eth0", "root", "netem", "delay", delay.toMillis() + "ms");
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to execute command", e);
                    }
                }
        );
    }

    @Override
    public void removeNetworkLatencyForResponses() {
        executeCommandInContainer(container -> {
            try {
                return container.execInContainer("tc", "qdisc", "del", "dev", "eth0", "root");
            } catch (Exception e) {
                throw new IllegalStateException("Failed to execute command", e);
            }
        });
    }

    @Override
    public void withNetworkLatency(Duration delay, ThrowingRunnable runnable) {
        try {
            addNetworkLatencyForResponses(delay);
            runnable.run();
        } catch (Exception e) {
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
            throw new RuntimeException(e);
        } finally {
            removeNetworkLatencyForResponses();
        }
    }

    private void executeCommandInContainer(Function<Container<?>, ExecResult> command) {
        Container.ExecResult execResult = command.apply(targetContainer);
        if (!execResult.getStderr().isEmpty()) {
            throw new IllegalStateException("Failed to execute command with message: " + execResult.getStderr());
        }
    }
}
