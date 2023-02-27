package com.playtika.test.mariadb;

import com.playtika.test.common.properties.InstallPackageProperties;
import com.playtika.test.common.utils.AptGetPackageInstaller;
import com.playtika.test.common.utils.PackageInstaller;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;

import static com.playtika.test.mariadb.MariaDBProperties.BEAN_NAME_EMBEDDED_MARIADB;

@AutoConfiguration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnBean({MariaDBProperties.class})
@ConditionalOnProperty(value = "embedded.mariadb.enabled", matchIfMissing = true)
public class EmbeddedMariaDBTestOperationsAutoConfiguration {

    @Bean
    @ConfigurationProperties("embedded.mariadb.install")
    public InstallPackageProperties mariadbPackageProperties() {
        return new InstallPackageProperties();
    }

    @Bean
    public PackageInstaller mariadbPackageInstaller(
            InstallPackageProperties mariadbPackageProperties,
            @Qualifier(BEAN_NAME_EMBEDDED_MARIADB) GenericContainer<?> mariadb
    ) {
        return new AptGetPackageInstaller(mariadbPackageProperties, mariadb);
    }
}
