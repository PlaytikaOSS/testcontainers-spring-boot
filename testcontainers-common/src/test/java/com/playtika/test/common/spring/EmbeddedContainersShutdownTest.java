package com.playtika.test.common.spring;

import com.playtika.test.bootstrap.EchoContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = EmbeddedContainersShutdownTest.Config.class)
public class EmbeddedContainersShutdownTest {

    @Autowired
    AllContainers allContainers;

    @Autowired
    EchoContainer echoContainer1;

    @Autowired
    EchoContainer echoContainer2;

    @Test
    void contextLoads() {
        assertThat(allContainers).isNotNull();
        assertThat(allContainers.getGenericContainers()).isNotNull();
        assertThat(allContainers.getGenericContainers()).isNotEmpty();
        assertThat(allContainers.getGenericContainers()).contains(echoContainer1);
        assertThat(allContainers.getGenericContainers()).contains(echoContainer2);
    }

    @EnableAutoConfiguration
    public static class Config {
    }
}
