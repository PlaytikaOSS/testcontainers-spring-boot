package com.playtika.test.aerospike;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.aerospike")
public class AerospikeProperties extends CommonContainerProperties {

    static final String AEROSPIKE_BEAN_NAME = "aerospike";

    boolean enabled = true;
    String namespace = "TEST";
    String host = "localhost";
    int port = 3000;
    String featureKey;

    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "aerospike:ce-6.2.0.2";
    }
}
