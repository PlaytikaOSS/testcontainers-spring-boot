package com.playtika.test.cassandra;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.cassandra")
public class CassandraProperties extends CommonContainerProperties {
    public static final String BEAN_NAME_EMBEDDED_CASSANDRA = "embeddedCassandra";
    public static final String DEFAULT_DATACENTER = "datacenter1";

    public String host = "localhost";
    public int port = 9042;
    public String keyspaceName = "embedded";
    public int replicationFactor = 1;

    // https://hub.docker.com/_/cassandra
    @Override
    public String getDefaultDockerImage() {
        return "cassandra:4.0";
    }
}
