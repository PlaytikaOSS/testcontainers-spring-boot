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
        return new InstallPackageProperties();
    }

    @Bean
    public PackageInstaller memsqlPackageInstaller(
            InstallPackageProperties memsqlPackageProperties,
            @Qualifier(BEAN_NAME_EMBEDDED_MEMSQL) GenericContainer<?> memsql
    ) {
        return new YumPackageInstaller(memsqlPackageProperties, memsql);
    }
}
