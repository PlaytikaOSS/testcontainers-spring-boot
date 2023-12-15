package com.playtika.testcontainer.aerospike;

import com.aerospike.client.AerospikeClient;
import com.playtika.testcontainer.common.properties.InstallPackageProperties;
import com.playtika.testcontainer.common.utils.AptGetPackageInstaller;
import com.playtika.testcontainer.common.utils.PackageInstaller;
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

import static com.playtika.testcontainer.aerospike.AerospikeProperties.BEAN_NAME_AEROSPIKE;
import static com.playtika.testcontainer.aerospike.AerospikeProperties.BEAN_NAME_AEROSPIKE_BEAN_NAME;

@AutoConfiguration(afterName = "org.springframework.boot.autoconfigure.aerospike.AerospikeAutoConfiguration")
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnBean({AerospikeClient.class, AerospikeProperties.class})
@ConditionalOnProperty(value = "embedded.aerospike.enabled", matchIfMissing = true)
public class EmbeddedAerospikeTestOperationsAutoConfiguration {

    @Bean(BEAN_NAME_AEROSPIKE_BEAN_NAME)
    @ConfigurationProperties("embedded.aerospike.install")
    public InstallPackageProperties aerospikePackageProperties() {
        return new InstallPackageProperties();
    }

    @Bean
    public PackageInstaller aerospikePackageInstaller(@Qualifier(BEAN_NAME_AEROSPIKE_BEAN_NAME) InstallPackageProperties aerospikePackageProperties,
                                                      @Qualifier(BEAN_NAME_AEROSPIKE) GenericContainer<?> aerospike) {
        return new AptGetPackageInstaller(aerospikePackageProperties, aerospike);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "embedded.aerospike.time-travel.enabled", havingValue = "true", matchIfMissing = true)
    public ExpiredDocumentsCleaner expiredDocumentsCleaner(AerospikeClient client,
                                                           AerospikeProperties properties) {
        return new AerospikeExpiredDocumentsCleaner(client, properties.getNamespace());
    }

    @Bean
    @ConditionalOnProperty(value = "embedded.aerospike.time-travel.enabled", havingValue = "false", matchIfMissing = false)
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
    @ConditionalOnMissingBean
    public AerospikeTestOperations aerospikeTestOperations(ExpiredDocumentsCleaner expiredDocumentsCleaner,
                                                           @Qualifier(BEAN_NAME_AEROSPIKE) GenericContainer<?> aerospike) {
        return new AerospikeTestOperations(expiredDocumentsCleaner, aerospike);
    }

}
