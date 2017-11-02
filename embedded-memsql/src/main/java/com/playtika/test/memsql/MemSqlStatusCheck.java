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
package com.playtika.test.memsql;

import com.github.dockerjava.api.DockerClient;
import com.playtika.test.common.checks.AbstractStartupCheckStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rnorth.ducttape.ratelimits.RateLimiter;
import org.rnorth.ducttape.ratelimits.RateLimiterBuilder;
import org.rnorth.ducttape.unreliables.Unreliables;

import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class MemSqlStatusCheck extends AbstractStartupCheckStrategy {

    private static final int MEMSQL_STARTUP_TIMEOUT = 120;

    private static final RateLimiter DOCKER_CLIENT_RATE_LIMITER = RateLimiterBuilder
            .newBuilder()
            .withRate(12, TimeUnit.MINUTES)
            .withConstantThroughput()
            .build();

    @Override
    public boolean waitUntilStartupSuccessful(DockerClient dockerClient, String containerId) {
        final Boolean[] startedOK = {null};
        Unreliables.retryUntilTrue(MEMSQL_STARTUP_TIMEOUT, TimeUnit.SECONDS, () -> {
            //noinspection CodeBlock2Expr
            return DOCKER_CLIENT_RATE_LIMITER.getWhenReady(() -> {
                StartupStatus state = checkStartupState(dockerClient, containerId);
                switch (state) {
                    case SUCCESSFUL:
                        startedOK[0] = true;
                        return true;
                    case FAILED:
                        startedOK[0] = false;
                        return true;
                    default:
                        return false;
                }
            });
        });
        return startedOK[0];
    }

    @Override
    public String[] getHealthCheckCmd() {
        return new String[]{
                "mysql",
                "-u",
                "root",
                "-h",
                "127.0.0.1",
                "-P",
                "3306",
                "--batch",
                "-e",
                "use test_db; SELECT 1;"
        };
    }
}
