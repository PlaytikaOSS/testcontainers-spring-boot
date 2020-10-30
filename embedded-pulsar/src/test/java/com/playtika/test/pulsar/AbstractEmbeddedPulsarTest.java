package com.playtika.test.pulsar;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@SpringBootTest(
        classes = AbstractEmbeddedPulsarTest.TestConfiguration.class,
        properties = {
                "embedded.pulsar.enabled=true"
        })
public abstract class AbstractEmbeddedPulsarTest {

    @Configuration
    @EnableAutoConfiguration
    static class TestConfiguration {
    }
}
