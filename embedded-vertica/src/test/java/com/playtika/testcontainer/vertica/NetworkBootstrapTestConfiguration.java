package com.playtika.testcontainer.vertica;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.Network;

@Configuration
@AutoConfigureBefore(EmbeddedVerticaBootstrapConfiguration.class)
public class NetworkBootstrapTestConfiguration {

    @Bean
    @ConditionalOnProperty(name = "embedded.vertica.custom-network")
    public Network network() {
        return Network.newNetwork();
    }

}
