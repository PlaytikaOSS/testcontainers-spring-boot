package com.playtika.test.pulsar;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        classes = TestConfiguration.class,
        properties = {
                "embedded.pulsar.enabled=true"
        })
public abstract class AbstractEmbeddedPulsarTest {
}
