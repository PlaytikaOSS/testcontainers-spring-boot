/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Playtika
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.playtika.test.elasticsearch;

import com.playtika.test.common.operations.DefaultNetworkTestOperations;
import com.playtika.test.common.operations.NetworkTestOperations;
import com.playtika.test.common.properties.InstallPackageProperties;
import com.playtika.test.common.utils.PackageInstaller;
import com.playtika.test.common.utils.YumPackageInstaller;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.util.Collections;

import static com.playtika.test.elasticsearch.ElasticSearchProperties.BEAN_NAME_EMBEDDED_ELASTIC_SEARCH;

@Configuration
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
