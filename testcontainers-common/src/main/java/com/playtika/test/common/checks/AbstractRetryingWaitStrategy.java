package com.playtika.test.common.checks;

import lombok.extern.slf4j.Slf4j;
import org.rnorth.ducttape.TimeoutException;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;

import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

@Slf4j
public abstract class AbstractRetryingWaitStrategy extends AbstractWaitStrategy {

    protected String getContainerType() {
        return getClass().getSimpleName();
    }

    @Override
    protected void waitUntilReady() {
        long seconds = startupTimeout.getSeconds();
        try {
            Unreliables.retryUntilTrue((int) seconds, TimeUnit.SECONDS,
                    () -> getRateLimiter().getWhenReady(this::isReady));
        } catch (TimeoutException e) {
            throw new ContainerLaunchException(
                    format("[%s] notifies that container[%s] is not ready after [%d] seconds, container cannot be started.",
                            getContainerType(), waitStrategyTarget.getContainerId(), seconds));
        }
    }

    protected abstract boolean isReady();

}