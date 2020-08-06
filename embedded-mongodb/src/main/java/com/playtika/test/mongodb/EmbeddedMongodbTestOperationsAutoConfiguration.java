package com.playtika.test.mongodb;

import com.playtika.test.common.operations.DefaultNetworkTestOperations;
import com.playtika.test.common.operations.NetworkTestOperations;
import com.playtika.test.common.properties.InstallPackageProperties;
import com.playtika.test.common.utils.AptGetPackageInstaller;
import com.playtika.test.common.utils.PackageInstaller;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.GenericContainer;

import java.util.Collections;

import static com.playtika.test.mongodb.MongodbProperties.BEAN_NAME_EMBEDDED_MONGODB;

@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnBean({MongodbProperties.class})
@ConditionalOnProperty(value = "embedded.mongodb.enabled", matchIfMissing = true)
public class EmbeddedMongodbTestOperationsAutoConfiguration {

    @Bean
    @ConfigurationProperties("embedded.mongodb.install")
    InstallPackageProperties mongodbPackageProperties() {
        InstallPackageProperties properties = new InstallPackageProperties();
        properties.setPackages(Collections.singleton("iproute2"));// we need iproute2 for tc command to work
        return properties;
    }

    @Bean
    PackageInstaller mongodbPackageInstaller(
            InstallPackageProperties mongodbPackageProperties,
            @Qualifier(BEAN_NAME_EMBEDDED_MONGODB) GenericContainer mongodb
    ) {
        return new AptGetPackageInstaller(mongodbPackageProperties, mongodb);
    }

    @Bean
    @ConditionalOnMissingBean(name = "mongodbNetworkTestOperations")
    public NetworkTestOperations mongodbNetworkTestOperations(
            @Qualifier(BEAN_NAME_EMBEDDED_MONGODB) GenericContainer mongodb
    ) {
        return new DefaultNetworkTestOperations(mongodb);
    }
}
