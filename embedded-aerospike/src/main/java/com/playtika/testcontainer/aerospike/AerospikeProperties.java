package com.playtika.testcontainer.aerospike;

import com.github.dockerjava.api.model.Capability;
import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.aerospike")
public class AerospikeProperties extends CommonContainerProperties {

    public static final String BEAN_NAME_AEROSPIKE = "aerospike";

    boolean enabled = true;
    String namespace = "TEST";
    String host = "localhost";
    int port = 3000;
    String featureKey;

    public AerospikeProperties() {
        this.setCapabilities(Arrays.asList(Capability.NET_ADMIN));
    }

    @Override
    public String getDefaultDockerImage() {
        return "aerospike/aerospike-server:6.2";
    }
}
