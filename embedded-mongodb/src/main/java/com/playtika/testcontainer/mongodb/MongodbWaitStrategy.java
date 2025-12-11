package com.playtika.testcontainer.mongodb;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

@Slf4j
@AllArgsConstructor
public class MongodbWaitStrategy extends AbstractWaitStrategy {

    private final MongodbProperties properties;

    @Override
    @SneakyThrows
    protected void waitUntilReady() {
        log.info("Waiting for mongodb to start");
        new LogMessageWaitStrategy().withRegEx(".*Waiting for connections.*").waitUntilReady(waitStrategyTarget);
        if (properties.getReplicaSetName() != null) {
            // The docker container will restart mongod and initialize the replicaset, so we just have to wait for that to finish now.
            log.info("Waiting for mongodb to become primary.");

            LogMessageWaitStrategy logMessageWaitStrategy = new LogMessageWaitStrategy().withRegEx(".*database writes are now permitted.*");
            logMessageWaitStrategy.waitUntilReady(waitStrategyTarget);
        }
    }
}
