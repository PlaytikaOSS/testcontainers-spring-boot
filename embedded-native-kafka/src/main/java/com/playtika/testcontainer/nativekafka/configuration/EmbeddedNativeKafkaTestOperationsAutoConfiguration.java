package com.playtika.testcontainer.nativekafka.configuration;

import com.playtika.testcontainer.common.properties.InstallPackageProperties;
import com.playtika.testcontainer.common.spring.ContainerStartupCoordinator;
import com.playtika.testcontainer.common.utils.PackageInstaller;
import com.playtika.testcontainer.common.utils.YumPackageInstaller;
import com.playtika.testcontainer.nativekafka.properties.NativeKafkaConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;

import static com.playtika.testcontainer.nativekafka.properties.NativeKafkaConfigurationProperties.NATIVE_KAFKA_BEAN_NAME;
import static com.playtika.testcontainer.nativekafka.properties.NativeKafkaConfigurationProperties.NATIVE_KAFKA_PACKAGE_PROPERTIES_BEAN_NAME;

@AutoConfiguration
@ConditionalOnBean({NativeKafkaConfigurationProperties.class})
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(value = {"embedded.kafka.enabled"}, havingValue = "true", matchIfMissing = true)
public class EmbeddedNativeKafkaTestOperationsAutoConfiguration {

    @Bean(NATIVE_KAFKA_PACKAGE_PROPERTIES_BEAN_NAME)
    @ConfigurationProperties("embedded.kafka.install")
    public InstallPackageProperties nativeKafkaPackageProperties() {
        return new InstallPackageProperties();
    }

    @Bean
    public PackageInstaller nativeKafkaPackageInstaller(ContainerStartupCoordinator startupCoordinator,
                                                       @Qualifier(NATIVE_KAFKA_PACKAGE_PROPERTIES_BEAN_NAME) InstallPackageProperties nativeKafkaPackageProperties,
                                                       @Qualifier(NATIVE_KAFKA_BEAN_NAME) GenericContainer<?> nativeKafka) {
        startupCoordinator.flush();
        return new YumPackageInstaller(nativeKafkaPackageProperties, nativeKafka);
    }
}