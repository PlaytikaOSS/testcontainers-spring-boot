/*
* The MIT License (MIT)
*
* Copyright (c) 2018 Playtika
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

import org.junit.Test;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.time.Duration;

public class AbstractCommandWaitStrategyTest {

    @Test
    public void should_passStartupChecks_andStartContainer() {
        startContainerWithWaitStrategy(new PositiveCommandWaitStrategy());
    }

    @Test(expected = ContainerLaunchException.class)
    public void should_failStartupChecks_ifHealthCheckCmdIsFailed() {
        startContainerWithWaitStrategy(new NegativeCommandWaitStrategy());
    }

    private void startContainerWithWaitStrategy(WaitStrategy waitStrategy) {
        new GenericContainer("alpine:latest")
                .withCommand("/bin/sh", "-c", "while true; do echo 'Press [CTRL+C] to stop..'; sleep 1; done")
                .waitingFor(waitStrategy)
                .withStartupTimeout(Duration.ofSeconds(15))
                .start();
    }

    private static class PositiveCommandWaitStrategy extends AbstractCommandWaitStrategy {

        @Override
        public String getContainerType() {
            return "Positive Test";
        }

        @Override
        public String[] getCheckCommand() {
            return new String[]{"echo", "health check passed"};
        }
    }

    private static class NegativeCommandWaitStrategy extends AbstractCommandWaitStrategy {

        @Override
        public String getContainerType() {
            return "Negative Test";
        }

        @Override
        public String[] getCheckCommand() {
            return new String[]{"/bin/sh", "-c", "invalid command"};
        }
    }
}