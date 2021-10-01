package com.playtika.test.neo4j;

import com.github.dockerjava.api.model.Capability;
import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.neo4j")
public class Neo4jProperties extends CommonContainerProperties {
    static final String BEAN_NAME_EMBEDDED_NEO4J = "embeddedNeo4j";
    // https://hub.docker.com/_/neo4j
    String dockerImage = "neo4j:4.3-community";
    String user = "neo4j";
    String password = "letmein";
    int httpsPort = 7473;
    int httpPort = 7474;
    int boltPort = 7687;

    public Neo4jProperties() {
        this.setCapabilities(Arrays.asList(Capability.NET_ADMIN));
    }
}
