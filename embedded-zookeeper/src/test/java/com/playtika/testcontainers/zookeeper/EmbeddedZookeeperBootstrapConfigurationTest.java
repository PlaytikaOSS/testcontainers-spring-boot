package com.playtika.testcontainers.zookeeper;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.ZooKeeper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(
        classes = EmbeddedZookeeperBootstrapConfigurationTest.TestConfiguration.class,
        properties = {
            "embedded.zookeeper.enabled=true"
        }
)
public class EmbeddedZookeeperBootstrapConfigurationTest extends EmbeddedZookeeperBootstrapConfigurationBaseTest {
    @Autowired
    private ZooKeeper zookeeper;
    @Test
    void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.zookeeper.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.zookeeper.admin.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.zookeeper.port")).isNotEmpty();
    }

    @Test
    void shouldConnectToRoot() throws Exception {
        List<String> list = zookeeper.getChildren("/", false);
        assertThat(list).isNotEmpty();
    }
}
