package com.playtika.testcontainers.aerospike.enterprise;

import com.aerospike.client.AerospikeClient;
import com.playtika.testcontainer.aerospike.AerospikeExpiredDocumentsCleaner;
import com.playtika.testcontainer.aerospike.AerospikeProperties;
import com.playtika.testcontainer.aerospike.EmbeddedAerospikeTestOperationsAutoConfiguration;
import com.playtika.testcontainer.aerospike.ExpiredDocumentsCleaner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(afterName = "org.springframework.boot.autoconfigure.aerospike.AerospikeAutoConfiguration",
        before = EmbeddedAerospikeTestOperationsAutoConfiguration.class)
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnBean({AerospikeClient.class, AerospikeProperties.class})
@ConditionalOnProperty(value = "embedded.aerospike.enabled", matchIfMissing = true)
public class EnterpriseAerospikeTestOperationsAutoConfiguration {

    @Bean
    @ConditionalOnProperty(value = "embedded.aerospike.time-travel.enabled", havingValue = "true", matchIfMissing = true)
    public ExpiredDocumentsCleaner expiredDocumentsCleaner(AerospikeClient client,
                                                           AerospikeProperties properties) {
        return new AerospikeExpiredDocumentsCleaner(client, properties.getNamespace(), true);
    }

}
