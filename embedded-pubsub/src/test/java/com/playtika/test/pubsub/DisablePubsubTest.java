package com.playtika.test.pubsub;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = DisablePubsubTest.TestConfiguration.class,
        properties = "embedded.google.pubsub.enabled=false")
public class DisablePubsubTest {
    @Autowired
    private ConfigurableEnvironment environment;

    @Test
    public void propertiesAreNotAvailable() {
        assertThat(environment.getProperty("embedded.google.pubsub.port")).isNullOrEmpty();
        assertThat(environment.getProperty("embedded.google.pubsub.host")).isNullOrEmpty();
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}