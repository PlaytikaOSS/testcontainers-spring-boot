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

import static com.playtika.testcontainer.minio.MinioProperties.BEAN_NAME_EMBEDDED_MINIO;
import static com.playtika.testcontainer.minio.MinioProperties.BEAN_NAME_EMBEDDED_MINIO_PACKAGE_PROPERTIES;

@AutoConfiguration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnBean({MinioProperties.class})
@ConditionalOnProperty(value = "embedded.minio.enabled", matchIfMissing = true)
public class EmbeddedMinioTestOperationsAutoConfiguration {

    @Bean(BEAN_NAME_EMBEDDED_MINIO_PACKAGE_PROPERTIES)
    @ConfigurationProperties("embedded.minio.install")
    public InstallPackageProperties minioPackageProperties() {
        return new InstallPackageProperties();
    }

    @Bean
    public PackageInstaller minioPackageInstaller(
            @Qualifier(BEAN_NAME_EMBEDDED_MINIO_PACKAGE_PROPERTIES) InstallPackageProperties minioPackageProperties,
            @Qualifier(BEAN_NAME_EMBEDDED_MINIO) GenericContainer<?> minio
    ) {
        return new MicroDnfPackageInstaller(minioPackageProperties, minio);
    }
}
