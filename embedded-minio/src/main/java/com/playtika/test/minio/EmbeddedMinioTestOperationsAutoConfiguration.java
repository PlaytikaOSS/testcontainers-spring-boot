package com.playtika.test.minio;

import com.playtika.test.common.operations.DefaultNetworkTestOperations;
import com.playtika.test.common.operations.NetworkTestOperations;
import com.playtika.test.common.properties.InstallPackageProperties;
import com.playtika.test.common.utils.ApkPackageInstaller;
import com.playtika.test.common.utils.PackageInstaller;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.GenericContainer;

import java.util.Collections;

@Configuration
@ConditionalOnBean({MinioProperties.class})
@ConditionalOnProperty(value = "embedded.minio.enabled", matchIfMissing = true)
public class EmbeddedMinioTestOperationsAutoConfiguration {

    @Bean
    @ConfigurationProperties("embedded.minio.install")
    InstallPackageProperties minioPackageProperties() {
        InstallPackageProperties properties = new InstallPackageProperties();
        properties.setPackages(Collections.singleton("iproute2"));// we need iproute2 for tc command to work
        return properties;
    }

    @Bean
    PackageInstaller minioPackageInstaller(InstallPackageProperties minioPackageProperties,
                                           GenericContainer minio) {
        return new ApkPackageInstaller(minioPackageProperties, minio);
    }

    @Bean
    @ConditionalOnMissingBean(name = "minioNetworkTestOperations")
    public NetworkTestOperations minioNetworkTestOperations(GenericContainer minio) {
        return new DefaultNetworkTestOperations(minio);
    }
}
