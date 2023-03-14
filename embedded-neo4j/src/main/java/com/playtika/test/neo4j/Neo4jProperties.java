package com.playtika.test.neo4j;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.neo4j")
public class Neo4jProperties extends CommonContainerProperties {
    static final String BEAN_NAME_EMBEDDED_NEO4J = "embeddedNeo4j";

    String user = "neo4j";
    String password = "letmein";
    int httpsPort = 7473;
    int httpPort = 7474;
    int boltPort = 7687;

    // https://hub.docker.com/_/neo4j
    @Override
    public String getDefaultDockerImage() {
        return "neo4j:4.4-community";
    }
}
