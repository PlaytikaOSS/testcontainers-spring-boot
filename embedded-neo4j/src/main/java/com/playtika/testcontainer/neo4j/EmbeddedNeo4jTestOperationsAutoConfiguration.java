package com.playtika.testcontainer.neo4j;

import com.playtika.testcontainer.common.properties.InstallPackageProperties;
import com.playtika.testcontainer.common.utils.AptGetPackageInstaller;
import com.playtika.testcontainer.common.utils.PackageInstaller;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;

import static com.playtika.testcontainer.neo4j.Neo4jProperties.BEAN_NAME_EMBEDDED_NEO4J;
import static com.playtika.testcontainer.neo4j.Neo4jProperties.BEAN_NAME_EMBEDDED_NEO4J_PACKAGE_PROPERTIES;

@AutoConfiguration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnBean({Neo4jProperties.class})
@ConditionalOnProperty(value = "embedded.neo4j.enabled", matchIfMissing = true)
public class EmbeddedNeo4jTestOperationsAutoConfiguration {

    @Bean(BEAN_NAME_EMBEDDED_NEO4J_PACKAGE_PROPERTIES)
    @ConfigurationProperties("embedded.neo4j.install")
    public InstallPackageProperties neo4jPackageProperties() {
        return new InstallPackageProperties();
    }

    @Bean()
    public PackageInstaller neo4jPackageInstaller(
            @Qualifier(BEAN_NAME_EMBEDDED_NEO4J_PACKAGE_PROPERTIES) InstallPackageProperties neo4jPackageProperties,
            @Qualifier(BEAN_NAME_EMBEDDED_NEO4J) GenericContainer<?> neo4j
    ) {
        return new AptGetPackageInstaller(neo4jPackageProperties, neo4j);
    }
}
