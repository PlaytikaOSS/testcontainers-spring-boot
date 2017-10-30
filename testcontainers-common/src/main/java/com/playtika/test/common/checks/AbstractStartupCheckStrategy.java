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
package com.playtika.test.common.checks;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.playtika.test.common.utils.ContainerUtils;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.startupcheck.StartupCheckStrategy;

import static com.playtika.test.common.utils.ContainerUtils.ExecCmdResult;

@Slf4j
public abstract class AbstractStartupCheckStrategy extends StartupCheckStrategy {

    public String getContainerType(){
        return getClass().getSimpleName();
    }

    public abstract String[] getHealthCheckCmd();

    @Override
    public StartupStatus checkStartupState(DockerClient dockerClient, String containerId) {
        String commandName = getContainerType();
        log.debug("{} execution for container id: {} ", commandName, containerId);

        InspectContainerResponse response = dockerClient.inspectContainerCmd(containerId).exec();
        if (!response.getState().getRunning()) {
            log.debug("{} Container {} is not started", commandName, containerId);
            return StartupStatus.NOT_YET_KNOWN;
        }

        ExecCmdResult healthCheckCmdResult = ContainerUtils.execCmd(dockerClient, containerId, getHealthCheckCmd());

        log.debug("{} executed with exitCode: {}, output: {}",
                commandName, healthCheckCmdResult.getExitCode(), healthCheckCmdResult.getOutput());

        if (healthCheckCmdResult.getExitCode() != 0) {
            log.debug("{} executed with exitCode !=0, considering status as unknown", commandName);
            return StartupStatus.NOT_YET_KNOWN;
        }
        log.debug("{} command executed, considering container {} successfully started", commandName, containerId);
        return StartupStatus.SUCCESSFUL;
    }
}