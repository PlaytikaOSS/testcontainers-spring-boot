package com.playtika.test.memsql;

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

import static com.playtika.test.memsql.MemSqlProperties.BEAN_NAME_EMBEDDED_MEMSQL;

@Configuration
@ConditionalOnBean({MemSqlProperties.class})
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(value = "embedded.memsql.enabled", matchIfMissing = true)
public class EmbeddedMemSqlTestOperationsAutoConfiguration {

    @Bean
    @ConfigurationProperties("embedded.memsql.install")
    public InstallPackageProperties memsqlPackageProperties() {
        InstallPackageProperties properties = new InstallPackageProperties();
        properties.setPackages(Collections.singleton("iproute2"));// we need iproute2 for tc command to work
        return properties;
    }

    @Bean
    public PackageInstaller memsqlPackageInstaller(
            InstallPackageProperties memsqlPackageProperties,
            @Qualifier(BEAN_NAME_EMBEDDED_MEMSQL) GenericContainer memsql
    ) {
        return new AptGetPackageInstaller(memsqlPackageProperties, memsql);
    }

    @Bean
    @ConditionalOnMissingBean(name = "memsqlNetworkTestOperations")
    public NetworkTestOperations memsqlNetworkTestOperations(
            @Qualifier(BEAN_NAME_EMBEDDED_MEMSQL) GenericContainer memsql
    ) {
        return new DefaultNetworkTestOperations(memsql);
    }
}
