package com.playtika.test.bootstrap;

import com.playtika.test.common.checks.PositiveCommandWaitStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.time.Duration;

@Slf4j
@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class EmbeddedMockServerBootstrapConfiguration {

    @Bean
    EchoContainer echoContainer1() {
        EchoContainer echoContainer = new EchoContainer()
                .withCommand("/bin/sh", "-c", "while true; do echo 'Press [CTRL+C] to stop..'; sleep 1; done")
                .waitingFor(new PositiveCommandWaitStrategy())
                .withStartupTimeout(Duration.ofSeconds(15));
        echoContainer.start();
        return echoContainer;
    }

    @Bean
    EchoContainer echoContainer2() {
        EchoContainer echoContainer = new EchoContainer()
                .withCommand("/bin/sh", "-c", "while true; do echo 'Press [CTRL+C] to stop..'; sleep 1; done")
                .waitingFor(new PositiveCommandWaitStrategy())
                .withStartupTimeout(Duration.ofSeconds(15));
        echoContainer.start();
        return echoContainer;
    }
}
