package com.playtika.testcontainer.couchbase;

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

import static com.playtika.testcontainer.couchbase.CouchbaseProperties.BEAN_NAME_EMBEDDED_COUCHBASE;

@AutoConfiguration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnBean({CouchbaseProperties.class})
@ConditionalOnProperty(value = "embedded.couchbase.enabled", matchIfMissing = true)
public class EmbeddedCouchbaseTestOperationsAutoConfiguration {

    @Bean
    @ConfigurationProperties("embedded.couchbase.install")
    public InstallPackageProperties couchbasePackageProperties() {
        return new InstallPackageProperties();
    }

    @Bean
    public PackageInstaller couchbasePackageInstaller(
            InstallPackageProperties couchbasePackageProperties,
            @Qualifier(BEAN_NAME_EMBEDDED_COUCHBASE) GenericContainer<?> couchbase
    ) {
        return new AptGetPackageInstaller(couchbasePackageProperties, couchbase);
    }
}
