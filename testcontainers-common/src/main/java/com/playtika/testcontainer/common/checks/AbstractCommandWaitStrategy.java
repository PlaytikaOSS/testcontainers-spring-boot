package com.playtika.testcontainer.common.checks;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.Container;

import java.util.Arrays;

import static com.playtika.testcontainer.common.utils.ContainerUtils.executeInContainer;

@Slf4j
public abstract class AbstractCommandWaitStrategy extends AbstractRetryingWaitStrategy {

    public abstract String[] getCheckCommand();

    protected boolean isReady() {
        String commandName = getContainerType();

        String containerId = waitStrategyTarget.getContainerId();
        String[] checkCommand = getCheckCommand();
        log.debug("{} execution of command {} for container id: {} ", commandName, Arrays.toString(checkCommand), containerId);

        Container.ExecResult healthCheckCmdResult = executeInContainer(waitStrategyTarget, checkCommand);

        log.debug("{} executed with result: {}", commandName, healthCheckCmdResult);

        if (healthCheckCmdResult.getExitCode() != 0) {
            log.debug("{} executed with exitCode !=0, considering status as unknown", commandName);
            return false;
        }
        log.debug("{} command executed, considering container {} successfully started", commandName, containerId);
        return true;
    }
}