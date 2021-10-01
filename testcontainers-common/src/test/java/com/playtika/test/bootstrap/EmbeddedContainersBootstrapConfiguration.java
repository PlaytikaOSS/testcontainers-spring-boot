package com.playtika.test.bootstrap;

import com.playtika.test.common.checks.PositiveCommandWaitStrategy;
import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
@AutoConfigureOrder
public class EmbeddedContainersBootstrapConfiguration {

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

    @Bean
    CommonContainerProperties properties(){
        CommonContainerProperties props = new CommonContainerProperties();
        props.setEnabled(true);
        return props;
    }
}
