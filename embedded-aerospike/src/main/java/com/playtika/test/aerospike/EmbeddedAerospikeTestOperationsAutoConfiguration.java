package com.playtika.test.aerospike;

import com.aerospike.client.AerospikeClient;
import com.playtika.test.common.operations.DefaultNetworkTestOperations;
import com.playtika.test.common.operations.NetworkTestOperations;
import com.playtika.test.common.properties.InstallPackageProperties;
import com.playtika.test.common.utils.AptGetPackageInstaller;
import com.playtika.test.common.utils.PackageInstaller;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.GenericContainer;

import java.util.Collections;

import static com.playtika.test.aerospike.AerospikeProperties.AEROSPIKE_BEAN_NAME;

@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnBean({AerospikeClient.class, AerospikeProperties.class})
@ConditionalOnProperty(value = "embedded.aerospike.enabled", matchIfMissing = true)
@AutoConfigureAfter(name = "org.springframework.boot.autoconfigure.aerospike.AerospikeAutoConfiguration")
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
            @Qualifier(AEROSPIKE_BEAN_NAME) GenericContainer aerospike
    ) {
        return new AptGetPackageInstaller(aerospikePackageProperties, aerospike);
    }

    @Bean
    @ConditionalOnMissingBean
    public ExpiredDocumentsCleaner expiredDocumentsCleaner(AerospikeClient client,
                                                           AerospikeProperties properties) {
        return new ExpiredDocumentsCleaner(client, properties.getNamespace());
    }

    @Bean
    @ConditionalOnMissingBean(name = "aerospikeNetworkTestOperations")
    public NetworkTestOperations aerospikeNetworkTestOperations(
            @Qualifier(AEROSPIKE_BEAN_NAME) GenericContainer aerospike
    ) {
        return new DefaultNetworkTestOperations(aerospike);
    }

    @Bean
    @ConditionalOnMissingBean
    public AerospikeTestOperations aerospikeTestOperations(ExpiredDocumentsCleaner expiredDocumentsCleaner,
                                                           NetworkTestOperations aerospikeNetworkTestOperations,
                                                           @Qualifier(AEROSPIKE_BEAN_NAME) GenericContainer aerospike) {
        return new AerospikeTestOperations(expiredDocumentsCleaner, aerospikeNetworkTestOperations, aerospike);
    }

    @Bean
    @ConditionalOnMissingBean
    public AerospikeTimeTravelService aerospikeTimeTravelService(AerospikeTestOperations aerospikeTestOperations) {
        return new AerospikeTimeTravelService(aerospikeTestOperations);
    }
}
