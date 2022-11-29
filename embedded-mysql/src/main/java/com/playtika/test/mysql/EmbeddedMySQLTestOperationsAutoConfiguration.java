package com.playtika.test.mysql;

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
import org.testcontainers.containers.MySQLContainer;

import java.util.Collections;

import static com.playtika.test.mysql.MySQLProperties.BEAN_NAME_EMBEDDED_MYSQL;

/**
 * Instead use ToxiProxy.
 */
@Deprecated
@AutoConfiguration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnBean({MySQLProperties.class})
@ConditionalOnProperty(value = "embedded.mysql.enabled", matchIfMissing = true)
public class EmbeddedMySQLTestOperationsAutoConfiguration {

    @Bean
    @ConfigurationProperties("embedded.mysql.install")
    public InstallPackageProperties mysqlPackageProperties() {
        InstallPackageProperties properties = new InstallPackageProperties();
        properties.setPackages(Collections.singleton("iproute2"));// we need iproute2 for tc command to work
        return properties;
    }

    @Bean
    public PackageInstaller mysqlPackageInstaller(
            InstallPackageProperties mysqlPackageProperties,
            @Qualifier(BEAN_NAME_EMBEDDED_MYSQL) MySQLContainer mysql
    ) {
        return new AptGetPackageInstaller(mysqlPackageProperties, mysql);
    }

    @Bean
    @ConditionalOnMissingBean(name = "mysqlNetworkTestOperations")
    public NetworkTestOperations mysqlNetworkTestOperations(
            @Qualifier(BEAN_NAME_EMBEDDED_MYSQL) MySQLContainer mysql
    ) {
        return new DefaultNetworkTestOperations(mysql);
    }
}
