package com.playtika.test.elasticsearch;

import com.playtika.test.common.operations.DefaultNetworkTestOperations;
import com.playtika.test.common.operations.NetworkTestOperations;
import com.playtika.test.common.properties.InstallPackageProperties;
import com.playtika.test.common.utils.PackageInstaller;
import com.playtika.test.common.utils.YumPackageInstaller;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.util.Collections;

import static com.playtika.test.elasticsearch.ElasticSearchProperties.BEAN_NAME_EMBEDDED_ELASTIC_SEARCH;

@AutoConfiguration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(value = "embedded.elasticsearch.install.enabled")
public class EmbeddedElasticSearchTestOperationsAutoConfiguration {

    @Bean
    @ConfigurationProperties("embedded.elasticsearch.install")
    public InstallPackageProperties elasticSearchPackageProperties() {
        InstallPackageProperties properties = new InstallPackageProperties();
        properties.setPackages(Collections.singleton("iproute"));// we need iproute for tc command to work
        return properties;
    }

    @Bean
    public PackageInstaller elasticSearchPackageInstaller(
            InstallPackageProperties elasticSearchPackageProperties,
            @Qualifier(BEAN_NAME_EMBEDDED_ELASTIC_SEARCH) ElasticsearchContainer elasticSearch) {
        return new YumPackageInstaller(elasticSearchPackageProperties, elasticSearch);
    }

    @Bean
    public NetworkTestOperations elasticSearchNetworkTestOperations(@Qualifier(BEAN_NAME_EMBEDDED_ELASTIC_SEARCH)
                                                                            ElasticsearchContainer elasticSearch) {
        return new DefaultNetworkTestOperations(elasticSearch);
    }
}
