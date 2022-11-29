package com.playtika.test.aerospike;

import com.aerospike.client.AerospikeClient;
import com.playtika.test.common.operations.DefaultNetworkTestOperations;
import com.playtika.test.common.operations.NetworkTestOperations;
import com.playtika.test.common.properties.InstallPackageProperties;
import com.playtika.test.common.utils.AptGetPackageInstaller;
import com.playtika.test.common.utils.PackageInstaller;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;

import java.time.Instant;
import java.util.Collections;

import static com.playtika.test.aerospike.AerospikeProperties.AEROSPIKE_BEAN_NAME;

@AutoConfiguration(afterName = "org.springframework.boot.autoconfigure.aerospike.AerospikeAutoConfiguration")
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnBean({AerospikeClient.class, AerospikeProperties.class})
@ConditionalOnProperty(value = "embedded.aerospike.enabled", matchIfMissing = true)
public class EmbeddedAerospikeTestOperationsAutoConfiguration {

    @Bean
    @ConfigurationProperties("embedded.aerospike.install")
    public InstallPackageProperties aerospikePackageProperties() {
        InstallPackageProperties properties = new InstallPackageProperties();
        properties.setPackages(Collections.singleton("iproute2"));// we need iproute2 for tc command to work
        return properties;
    }

    @Bean
    public PackageInstaller aerospikePackageInstaller(
            InstallPackageProperties aerospikePackageProperties,
            @Qualifier(AEROSPIKE_BEAN_NAME) GenericContainer<?> aerospike
    ) {
        return new AptGetPackageInstaller(aerospikePackageProperties, aerospike);
    }

    @Bean
    @ConditionalOnProperty(value = "embedded.aerospike.time-travel.enabled", havingValue="true", matchIfMissing = true)
    public ExpiredDocumentsCleaner expiredDocumentsCleaner(AerospikeClient client,
                                                                    AerospikeProperties properties) {
        return new AerospikeExpiredDocumentsCleaner(client, properties.getNamespace());
    }

    @Bean
    @ConditionalOnProperty(value = "embedded.aerospike.time-travel.enabled", havingValue="false", matchIfMissing = false)
    public ExpiredDocumentsCleaner disabledExpiredDocumentsCleaner() {
        return new ExpiredDocumentsCleaner() {
            @Override
            public void cleanExpiredDocumentsBefore(long millis) {
                throw new UnsupportedOperationException("Expired documents cleaner is disabled. Change property embedded.aerospike.time-travel.enabled to enable it.");
            }

            @Override
            public void cleanExpiredDocumentsBefore(Instant expireTime) {
                throw new UnsupportedOperationException("Expired documents cleaner is disabled. Change property embedded.aerospike.time-travel.enabled to enable it.");
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(name = "aerospikeNetworkTestOperations")
    public NetworkTestOperations aerospikeNetworkTestOperations(
            @Qualifier(AEROSPIKE_BEAN_NAME) GenericContainer<?> aerospike
    ) {
        return new DefaultNetworkTestOperations(aerospike);
    }

    @Bean
    @ConditionalOnMissingBean
    public AerospikeTestOperations aerospikeTestOperations(ExpiredDocumentsCleaner expiredDocumentsCleaner,
                                                           NetworkTestOperations aerospikeNetworkTestOperations,
                                                           @Qualifier(AEROSPIKE_BEAN_NAME) GenericContainer<?> aerospike) {
        return new AerospikeTestOperations(expiredDocumentsCleaner, aerospikeNetworkTestOperations, aerospike);
    }

}
