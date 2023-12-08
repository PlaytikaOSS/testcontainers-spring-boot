package com.playtika.testcontainer.kafka.configuration;

import com.playtika.testcontainer.common.properties.InstallPackageProperties;
import com.playtika.testcontainer.common.utils.PackageInstaller;
import com.playtika.testcontainer.common.utils.YumPackageInstaller;
import com.playtika.testcontainer.kafka.properties.KafkaConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;

import static com.playtika.testcontainer.kafka.properties.KafkaConfigurationProperties.KAFKA_BEAN_NAME;
import static com.playtika.testcontainer.kafka.properties.KafkaConfigurationProperties.KAFKA_PACKAGE_PROPERTIES_BEAN_NAME;

@AutoConfiguration
@ConditionalOnBean({KafkaConfigurationProperties.class})
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(value = {"embedded.kafka.enabled"}, havingValue = "true", matchIfMissing = true)
public class EmbeddedKafkaTestOperationsAutoConfiguration {

    @Bean(KAFKA_PACKAGE_PROPERTIES_BEAN_NAME)
    @ConfigurationProperties("embedded.kafka.install")
    public InstallPackageProperties kafkaPackageProperties() {
        return new InstallPackageProperties();
    }

    @Bean
    public PackageInstaller kafkaPackageInstaller(@Qualifier(KAFKA_PACKAGE_PROPERTIES_BEAN_NAME) InstallPackageProperties kafkaPackageProperties,
                                                  @Qualifier(KAFKA_BEAN_NAME) GenericContainer<?> kafka) {
        return new YumPackageInstaller(kafkaPackageProperties, kafka);
    }
}
