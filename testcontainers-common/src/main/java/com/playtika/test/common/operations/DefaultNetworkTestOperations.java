/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Playtika
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
    private final GenericContainer targetContainer;

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

}
