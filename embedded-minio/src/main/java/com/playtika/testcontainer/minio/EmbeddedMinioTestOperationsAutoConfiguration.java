package com.playtika.testcontainer.minio;

import com.playtika.testcontainer.common.properties.InstallPackageProperties;
import com.playtika.testcontainer.common.utils.MicroDnfPackageInstaller;
import com.playtika.testcontainer.common.utils.PackageInstaller;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;

import static com.playtika.testcontainer.minio.MinioProperties.MINIO_BEAN_NAME;

@AutoConfiguration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnBean({MinioProperties.class})
@ConditionalOnProperty(value = "embedded.minio.enabled", matchIfMissing = true)
public class EmbeddedMinioTestOperationsAutoConfiguration {

    @Bean
    @ConfigurationProperties("embedded.minio.install")
    InstallPackageProperties minioPackageProperties() {
        return new InstallPackageProperties();
    }

    @Bean
    PackageInstaller minioPackageInstaller(
            InstallPackageProperties minioPackageProperties,
            @Qualifier(MINIO_BEAN_NAME) GenericContainer<?> minio
    ) {
        return new MicroDnfPackageInstaller(minioPackageProperties, minio);
    }
}
