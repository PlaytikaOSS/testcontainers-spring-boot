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
import lombok.Builder;
import org.testcontainers.containers.startupcheck.StartupCheckStrategy;

import java.util.LinkedList;
import java.util.List;

@Builder
public class CompositeStartupCheckStrategy extends StartupCheckStrategy {

    List<StartupCheckStrategy> checksToPerform = new LinkedList<>();

    @Override
    public StartupStatus checkStartupState(DockerClient dockerClient, String containerId) {
        for (StartupCheckStrategy check : checksToPerform) {
            StartupStatus startupStatus = check.checkStartupState(dockerClient, containerId);
            if (startupStatus != StartupStatus.SUCCESSFUL) {
                return StartupStatus.NOT_YET_KNOWN;
            }
        }
        return StartupStatus.SUCCESSFUL;
    }
}
