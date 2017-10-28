package com.playtika.test.aerospike;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("embedded.aerospike")
public class AerospikeProperties {

    static final String AEROSPIKE_BEAN_NAME = "aerospike";

    boolean enabled = true;
    String dockerImage = "aerospike:3.15.0.1";
    final String namespace = "TEST";
    final int port = 3000;
}
