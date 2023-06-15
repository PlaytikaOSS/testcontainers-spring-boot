package com.playtika.testcontainers.zookeeper;

import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.testcontainers.containers.GenericContainer;

import java.util.concurrent.CountDownLatch;

public abstract class EmbeddedZookeeperBootstrapConfigurationBaseTest {
    @Autowired
    protected ConfigurableEnvironment environment;

    @Autowired
    protected GenericContainer<?> zooKeeperContainer;

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
        private static final int DEFAULT_SESSION_TIMEOUT_MS = 60_000;

        @Bean(destroyMethod = "close")
        public ZooKeeper zookeeperClient(@Value("${embedded.zookeeper.host}") String host,
                                         @Value("${embedded.zookeeper.port}") int port) throws Exception {
            CountDownLatch connSignal = new CountDownLatch(1);
            String connectionString = host + ":" + port;
            ZooKeeper zooKeeper = new ZooKeeper(connectionString, DEFAULT_SESSION_TIMEOUT_MS, event -> {
                if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    connSignal.countDown();
                }
            });
            connSignal.await();
            return zooKeeper;
        }
    }
}
