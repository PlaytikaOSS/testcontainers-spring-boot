package com.playtika.testcontainer.keydb;

import com.playtika.testcontainer.common.properties.InstallPackageProperties;
import com.playtika.testcontainer.common.utils.ApkPackageInstaller;
import com.playtika.testcontainer.common.utils.PackageInstaller;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;

import static com.playtika.testcontainer.keydb.KeyDbProperties.BEAN_NAME_EMBEDDED_KEYDB;
import static com.playtika.testcontainer.keydb.KeyDbProperties.BEAN_NAME_EMBEDDED_KEYDB_PACKAGE_PROPERTIES;

@AutoConfiguration
@ConditionalOnBean({KeyDbProperties.class})
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(value = "embedded.keydb.enabled", matchIfMissing = true)
public class EmbeddedKeyDbTestOperationsAutoConfiguration {

  @Bean(BEAN_NAME_EMBEDDED_KEYDB_PACKAGE_PROPERTIES)
  @ConfigurationProperties("embedded.keydb.install")
  public InstallPackageProperties keyDbPackageProperties() {
    return new InstallPackageProperties();
  }

  @Bean
  public PackageInstaller keyDbPackageInstaller(
    @Qualifier(BEAN_NAME_EMBEDDED_KEYDB_PACKAGE_PROPERTIES) InstallPackageProperties keyDbPackageProperties,
    @Qualifier(BEAN_NAME_EMBEDDED_KEYDB) GenericContainer<?> keyDb
  ) {
    return new ApkPackageInstaller(keyDbPackageProperties, keyDb);
  }

}
