package com.playtika.test.common.checks;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.time.Duration;

public class AbstractCommandWaitStrategyTest {

    @Test
    public void should_passStartupChecks_andStartContainer() {
        startContainerWithWaitStrategy(new PositiveCommandWaitStrategy());
    }

    @Test
    public void should_failStartupChecks_ifHealthCheckCmdIsFailed() {
        Assertions.assertThrows(
                ContainerLaunchException.class,
                () -> startContainerWithWaitStrategy(new NegativeCommandWaitStrategy())
        );
    }

    private void startContainerWithWaitStrategy(WaitStrategy waitStrategy) {
        new GenericContainer("alpine:latest")
                .withCommand("/bin/sh", "-c", "while true; do echo 'Press [CTRL+C] to stop..'; sleep 1; done")
                .waitingFor(waitStrategy)
                .withStartupTimeout(Duration.ofSeconds(15))
                .start();
    }

}