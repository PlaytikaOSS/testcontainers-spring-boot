package com.playtika.testcontainer.rabbitmq;

import com.rabbitmq.stream.Environment;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@SpringBootTest(
        classes = EmbeddedRabbitMQAdditionalPortsTest.TestConfiguration.class
)
@ActiveProfiles({"enabled", "stream"})
public class EmbeddedRabbitMQAdditionalPortsTest {

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }

    @Autowired
    Environment environment;

    @Test
    void streamPortExposed() {
        environment.streamCreator().name("stream").create();
        environment.streamExists("stream");
    }
}
