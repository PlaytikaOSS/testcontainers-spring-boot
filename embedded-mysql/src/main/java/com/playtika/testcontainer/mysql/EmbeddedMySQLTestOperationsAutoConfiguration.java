package com.playtika.testcontainer.mysql;

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
import org.testcontainers.containers.MySQLContainer;

import static com.playtika.testcontainer.mysql.MySQLProperties.BEAN_NAME_EMBEDDED_MYSQL;

@AutoConfiguration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnBean({MySQLProperties.class})
@ConditionalOnProperty(value = "embedded.mysql.enabled", matchIfMissing = true)
public class EmbeddedMySQLTestOperationsAutoConfiguration {

    @Bean
    @ConfigurationProperties("embedded.mysql.install")
    public InstallPackageProperties mysqlPackageProperties() {
        return new InstallPackageProperties();
    }

    @Bean
    public PackageInstaller mysqlPackageInstaller(
            InstallPackageProperties mysqlPackageProperties,
            @Qualifier(BEAN_NAME_EMBEDDED_MYSQL) MySQLContainer mysql
    ) {
        return new AptGetPackageInstaller(mysqlPackageProperties, mysql);
    }
}
