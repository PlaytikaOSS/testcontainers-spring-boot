package com.playtika.test.minio;

import com.playtika.test.common.properties.InstallPackageProperties;
import com.playtika.test.common.utils.MicroDnfPackageInstaller;
import com.playtika.test.common.utils.PackageInstaller;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;

import static com.playtika.test.minio.MinioProperties.MINIO_BEAN_NAME;

@AutoConfiguration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnBean({MinioProperties.class})
@ConditionalOnProperty(value = "embedded.minio.enabled", matchIfMissing = true)
public class EmbeddedMinioTestOperationsAutoConfiguration {

    @Bean
    @ConfigurationProperties("embedded.minio.install")
    InstallPackageProperties minioPackageProperties() {
        InstallPackageProperties properties = new InstallPackageProperties();
//        properties.setPackages(Collections.singleton("iproute2"));// we need iproute2 for tc command to work
        return properties;
    }

    @Bean
    PackageInstaller minioPackageInstaller(
            InstallPackageProperties minioPackageProperties,
            @Qualifier(MINIO_BEAN_NAME) GenericContainer<?> minio
    ) {
        return new MicroDnfPackageInstaller(minioPackageProperties, minio);
    }

// Current image doesn't support `tc` command, since Minio is currently based on ubi-minimal with microdnf package manager. `iproute2` package is not available here.
// This bean is commented, so that users that expect NetworkTestOperations in the tests are notified that this is not supported anymore.
//    @Bean
//    @ConditionalOnMissingBean(name = "minioNetworkTestOperations")
//    public NetworkTestOperations minioNetworkTestOperations(
//            @Qualifier(MINIO_BEAN_NAME) GenericContainer<?> minio
//    ) {
//        return new DefaultNetworkTestOperations(minio);
//    }
}
