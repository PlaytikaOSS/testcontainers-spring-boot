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