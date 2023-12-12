package com.playtika.testcontainer.enterpise.aerospike;

import com.playtika.testcontainer.aerospike.AerospikeProperties;
import com.playtika.testcontainer.aerospike.EmbeddedAerospikeBootstrapConfiguration;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;

@Slf4j
@AutoConfiguration(after = EmbeddedAerospikeBootstrapConfiguration.class)
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(value = "embedded.aerospike.enabled", matchIfMissing = true)
public class SetupEnterpriseAerospikeBootstrapConfiguration {

    private GenericContainer<?> aerospikeContainer;
    private AerospikeProperties aerospikeProperties;

    @Autowired
    @Qualifier(AerospikeProperties.BEAN_NAME_AEROSPIKE)
    public void setAerospikeContainer(GenericContainer<?> aerospikeContainer) {
        this.aerospikeContainer = aerospikeContainer;
    }

    @Autowired
    public void setAerospikeProperties(AerospikeProperties aerospikeProperties) {
        this.aerospikeProperties = aerospikeProperties;
    }

    @PostConstruct
    public void setupEnterpriseAerospike() throws IOException, InterruptedException {
        log.info("Setting up disallow-expunge to true");
        String namespace = aerospikeProperties.getNamespace();
        Container.ExecResult result = aerospikeContainer.execInContainer("asadm", "-e",
                String.format("enable; manage config namespace %s param disallow-expunge to true", namespace));
        log.info("Set up disallow-expunge to true: {}", result.getStdout());
    }

}
