package com.playtika.testcontainer.minio;

import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;

public class DefaultMinioWaitStrategy extends WaitAllStrategy implements MinioWaitStrategy {

    public DefaultMinioWaitStrategy(MinioProperties properties) {
        withStrategy(new HostPortWaitStrategy())
                .withStrategy(new HttpWaitStrategy()
                        .forPath("/minio/health/live")
                        .forPort(properties.port)
                        .forStatusCode(200))
                .withStartupTimeout(properties.getTimeoutDuration());
    }
}
