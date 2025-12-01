package com.playtika.testcontainer.artifactory;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.postgresql.EmbeddedPostgreSQLBootstrapConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.Network;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(name = "embedded.artifactory.enabled", matchIfMissing = true)
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@AutoConfigureBefore(EmbeddedPostgreSQLBootstrapConfiguration.class)
public class EmbeddedArtifactoryNetworkBootstrapConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(Network.class)
    public Network network() {
        Network network = Network.newNetwork();
        log.info("Created docker Network with id={}", network.getId());
        return network;
    }
}
