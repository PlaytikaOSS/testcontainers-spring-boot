package com.playtika.testcontainers.aerospike.enterprise;

import com.playtika.testcontainer.aerospike.AerospikeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
public class AerospikeEnterpriseConfigurer {

    private final AerospikeProperties aerospikeProperties;
    private final AerospikeEnterpriseProperties enterpriseProperties;

    public void configure(GenericContainer<?> aerospikeContainer) throws IOException, InterruptedException {
        if (aerospikeProperties.getFeatureKey() == null || aerospikeProperties.getFeatureKey().isBlank()) {
            log.warn("Evaluation feature key file not provided by 'embedded.aerospike.featureKey' property. " +
                    "Pay attention to license details: https://github.com/aerospike/aerospike-server.docker/blob/master/enterprise/ENTERPRISE_LICENSE");
        }

        String namespace = aerospikeProperties.getNamespace();
        AsadmCommandExecutor asadmCommandExecutor = new AsadmCommandExecutor(aerospikeContainer);

        /*
         By default, the value of this metric is 90%, we set it to 100% to prevent stopping writes for the Aerospike
         Enterprise container during high consumption of system memory. For the Aerospike Community Edition, this metric is not used.
         Documentation: https://aerospike.com/docs/server/reference/configuration#stop-writes-sys-memory-pct
        */
        log.info("Switching off 'stop-writes-sys-memory-pct'... ");
        asadmCommandExecutor.execute(String.format("manage config namespace %s param stop-writes-sys-memory-pct to 100", namespace));
        log.info("Success switching off 'stop-writes-sys-memory-pct'");

        if (enterpriseProperties.isDurableDeletes()) {
            log.info("Setting up 'disallow-expunge' to true...");
            asadmCommandExecutor.execute(String.format("manage config namespace %s param disallow-expunge to true", namespace));
            log.info("Success setting up 'disallow-expunge' to true");
        }
    }

}
