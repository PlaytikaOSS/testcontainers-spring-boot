package com.playtika.test.memsql;

import com.playtika.test.common.properties.InstallPackageProperties;
import com.playtika.test.common.utils.PackageInstaller;
import com.playtika.test.common.utils.YumPackageInstaller;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;

import static com.playtika.test.memsql.MemSqlProperties.BEAN_NAME_EMBEDDED_MEMSQL;

@AutoConfiguration
@ConditionalOnBean({MemSqlProperties.class})
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(value = "embedded.memsql.enabled", matchIfMissing = true)
public class EmbeddedMemSqlTestOperationsAutoConfiguration {

    @Bean
    @ConfigurationProperties("embedded.memsql.install")
    public InstallPackageProperties memsqlPackageProperties() {
        InstallPackageProperties properties = new InstallPackageProperties();
//        properties.setPackages(Collections.singleton("iproute2"));// we need iproute2 for tc command to work
        return properties;
    }

    @Bean
    public PackageInstaller memsqlPackageInstaller(
            InstallPackageProperties memsqlPackageProperties,
            @Qualifier(BEAN_NAME_EMBEDDED_MEMSQL) GenericContainer<?> memsql
    ) {
        return new YumPackageInstaller(memsqlPackageProperties, memsql);
    }

// Current image doesn't support `tc` command, since memsql is currently based on centos with yum package manager. `iproute2` package is not available here.
// This bean is commented, so that users that expect NetworkTestOperations in the tests are notified that this is not supported anymore.
//    @Bean
//    @ConditionalOnMissingBean(name = "memsqlNetworkTestOperations")
//    public NetworkTestOperations memsqlNetworkTestOperations(
//            @Qualifier(BEAN_NAME_EMBEDDED_MEMSQL) GenericContainer<?> memsql
//    ) {
//        return new DefaultNetworkTestOperations(memsql);
//    }
}
