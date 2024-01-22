package com.playtika.testcontainers.aerospike.enterprise;

import com.playtika.testcontainer.aerospike.AerospikeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
public class AerospikeEnterpriseConfigurer {

    private final AerospikeProperties aerospikeProperties;
    private final AerospikeEnterpriseProperties enterpriseProperties;

    public void configure(GenericContainer<?> aerospikeContainer)  throws IOException, InterruptedException {
        if (aerospikeProperties.getFeatureKey() == null || aerospikeProperties.getFeatureKey().isBlank()) {
            log.warn("Evaluation feature key file not provided by 'embedded.aerospike.featureKey' property. " +
                    "Pay attention to license details: https://github.com/aerospike/aerospike-server.docker/blob/master/enterprise/ENTERPRISE_LICENSE");
        }

        setupDisallowExpunge(aerospikeContainer);
    }

    private void setupDisallowExpunge(GenericContainer<?> aerospikeContainer) throws IOException, InterruptedException {
        if (!enterpriseProperties.isDurableDeletes()) {
            return;
        }
        log.info("Setting up 'disallow-expunge' to true...");
        String namespace = aerospikeProperties.getNamespace();
        Container.ExecResult result = aerospikeContainer.execInContainer("asadm", "-e",
                String.format("enable; manage config namespace %s param disallow-expunge to true", namespace));
        if (result.getStderr().length() > 0) {
            throw new IllegalStateException("Failed to set up 'disallow-expunge' to true: " + result.getStderr());
        }
        log.info("Set up 'disallow-expunge' to true: {}", result.getStdout());
    }
}
