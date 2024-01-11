package com.playtika.testcontainers.aerospike.enterprise;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("embedded.aerospike.enterprise")
public class AerospikeEnterpriseProperties {

    boolean durableDeletes = true;
}
