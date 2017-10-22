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
package com.playtika.test.kafka.checks;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.playtika.test.kafka.utils.ContainerUtils;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.startupcheck.StartupCheckStrategy;

import static com.playtika.test.kafka.utils.ContainerUtils.ExecCmdResult;

@Slf4j
public abstract class AbstractStartupCheckStrategy extends StartupCheckStrategy {

    abstract String getContainerType();
    abstract String[] getHealthCheckCmd();

    @Override
    public StartupStatus checkStartupState(DockerClient dockerClient, String containerId) {
        String containerType = getContainerType();
        log.info("Check {} container status, id: {} ", containerType, containerId);

        InspectContainerResponse response = dockerClient.inspectContainerCmd(containerId).exec();
        if (!response.getState().getRunning()) {
            log.error("{} failed to start", containerType);
            return StartupStatus.FAILED;
        }

        ExecCmdResult healthCheckCmdResult = ContainerUtils.execCmd(dockerClient, containerId, getHealthCheckCmd());

        if (healthCheckCmdResult.getExitCode() != 0) {
            log.error("{} health check failed with exitCode: {}, output: {}",
                    containerType, healthCheckCmdResult.getExitCode(), healthCheckCmdResult.getOutput());
            return StartupStatus.FAILED;
        }

        log.info("{} container is successfully started", containerType);
        return StartupStatus.SUCCESSFUL;
    }
}