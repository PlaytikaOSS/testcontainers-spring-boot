package com.playtika.test.couchbase;

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

import static com.playtika.test.couchbase.CouchbaseProperties.BEAN_NAME_EMBEDDED_COUCHBASE;

@AutoConfiguration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnBean({CouchbaseProperties.class})
@ConditionalOnProperty(value = "embedded.couchbase.enabled", matchIfMissing = true)
public class EmbeddedCouchbaseTestOperationsAutoConfiguration {

    @Bean
    @ConfigurationProperties("embedded.couchbase.install")
    public InstallPackageProperties couchbasePackageProperties() {
        InstallPackageProperties properties = new InstallPackageProperties();
        properties.setPackages(Collections.singleton("iproute2"));// we need iproute2 for tc command to work
        return properties;
    }

    @Bean
    public PackageInstaller couchbasePackageInstaller(
            InstallPackageProperties couchbasePackageProperties,
            @Qualifier(BEAN_NAME_EMBEDDED_COUCHBASE) GenericContainer<?> couchbase
    ) {
        return new AptGetPackageInstaller(couchbasePackageProperties, couchbase);
    }

    @Bean
    @ConditionalOnMissingBean(name = "couchbaseNetworkTestOperations")
    public NetworkTestOperations couchbaseNetworkTestOperations(
            @Qualifier(BEAN_NAME_EMBEDDED_COUCHBASE) GenericContainer<?> couchbase
    ) {
        return new DefaultNetworkTestOperations(couchbase);
    }
}
