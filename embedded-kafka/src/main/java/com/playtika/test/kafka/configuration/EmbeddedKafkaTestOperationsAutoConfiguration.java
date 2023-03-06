package com.playtika.test.kafka.configuration;

import com.playtika.test.common.properties.InstallPackageProperties;
import com.playtika.test.common.utils.PackageInstaller;
import com.playtika.test.common.utils.YumPackageInstaller;
import com.playtika.test.kafka.properties.KafkaConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;

import static com.playtika.test.kafka.properties.KafkaConfigurationProperties.KAFKA_BEAN_NAME;

@AutoConfiguration
@ConditionalOnBean({KafkaConfigurationProperties.class})
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(value = {"embedded.kafka.enabled"}, havingValue = "true", matchIfMissing = true)
public class EmbeddedKafkaTestOperationsAutoConfiguration {

    @Bean
    @ConfigurationProperties("embedded.kafka.install")
    public InstallPackageProperties kafkaPackageProperties() {
        return new InstallPackageProperties();
    }

    @Bean
    public PackageInstaller kafkaPackageInstaller(InstallPackageProperties kafkaPackageProperties,
                                                  @Qualifier(KAFKA_BEAN_NAME) GenericContainer<?> kafka) {
        return new YumPackageInstaller(kafkaPackageProperties, kafka);
    }
}
