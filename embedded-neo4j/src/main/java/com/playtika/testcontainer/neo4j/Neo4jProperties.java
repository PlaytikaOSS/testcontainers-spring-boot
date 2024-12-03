package com.playtika.testcontainer.neo4j;

import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.neo4j")
public class Neo4jProperties extends CommonContainerProperties {
    static final String BEAN_NAME_EMBEDDED_NEO4J = "embeddedNeo4j";
    static final String BEAN_NAME_EMBEDDED_NEO4J_PACKAGE_PROPERTIES = "neo4jPackageProperties";

    String user = "neo4j";
    String password = "password";
    int httpsPort = 7473;
    int httpPort = 7474;
    int boltPort = 7687;

    // https://hub.docker.com/_/neo4j
    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "neo4j:5.25-community";
    }
}
