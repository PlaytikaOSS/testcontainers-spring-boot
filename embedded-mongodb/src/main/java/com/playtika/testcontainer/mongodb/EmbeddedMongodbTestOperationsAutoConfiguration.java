package com.playtika.testcontainer.mongodb;

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

import static com.playtika.testcontainer.mongodb.MongodbProperties.BEAN_NAME_EMBEDDED_MONGODB;

@AutoConfiguration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnBean({MongodbProperties.class})
@ConditionalOnProperty(value = "embedded.mongodb.enabled", matchIfMissing = true)
public class EmbeddedMongodbTestOperationsAutoConfiguration {

    @Bean
    @ConfigurationProperties("embedded.mongodb.install")
    InstallPackageProperties mongodbPackageProperties() {
        return new InstallPackageProperties();
    }

    @Bean
    PackageInstaller mongodbPackageInstaller(
            InstallPackageProperties mongodbPackageProperties,
            @Qualifier(BEAN_NAME_EMBEDDED_MONGODB) GenericContainer<?> mongodb
    ) {
        return new AptGetPackageInstaller(mongodbPackageProperties, mongodb);
    }
}
