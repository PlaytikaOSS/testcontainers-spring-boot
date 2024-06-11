package com.playtika.testcontainer.aerospike;

import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.aerospike")
public class AerospikeProperties extends CommonContainerProperties {

    public static final String BEAN_NAME_AEROSPIKE = "aerospike";
    static final String BEAN_NAME_AEROSPIKE_BEAN_NAME = "aerospikePackageProperties";

    boolean enabled = true;
    String namespace = "TEST";
    String host = "localhost";
    int port = 3000;
    String featureKey;

    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "aerospike/aerospike-server:7.1.0.1";
    }
}
