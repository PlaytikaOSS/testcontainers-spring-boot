package com.playtika.test.neo4j;

import com.playtika.test.common.operations.DefaultNetworkTestOperations;
import com.playtika.test.common.operations.NetworkTestOperations;
import com.playtika.test.common.properties.InstallPackageProperties;
import com.playtika.test.common.utils.AptGetPackageInstaller;
import com.playtika.test.common.utils.PackageInstaller;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;

import java.util.Collections;

import static com.playtika.test.neo4j.Neo4jProperties.BEAN_NAME_EMBEDDED_NEO4J;

@AutoConfiguration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnBean({Neo4jProperties.class})
@ConditionalOnProperty(value = "embedded.neo4j.enabled", matchIfMissing = true)
public class EmbeddedNeo4jTestOperationsAutoConfiguration {

    @Bean
    @ConfigurationProperties("embedded.neo4j.install")
    public InstallPackageProperties neo4jPackageProperties() {
        InstallPackageProperties properties = new InstallPackageProperties();
        properties.setPackages(Collections.singleton("iproute2"));// we need iproute2 for tc command to work
        return properties;
    }

    @Bean
    public PackageInstaller neo4jPackageInstaller(
            InstallPackageProperties neo4jPackageProperties,
            @Qualifier(BEAN_NAME_EMBEDDED_NEO4J) GenericContainer<?> neo4j
    ) {
        return new AptGetPackageInstaller(neo4jPackageProperties, neo4j);
    }

    @Bean
    @ConditionalOnMissingBean(name = "neo4jNetworkTestOperations")
    public NetworkTestOperations neo4jNetworkTestOperations(
            @Qualifier(BEAN_NAME_EMBEDDED_NEO4J) GenericContainer<?> neo4j
    ) {
        return new DefaultNetworkTestOperations(neo4j);
    }
}
