/*
* The MIT License (MIT)
*
* Copyright (c) 2018 Playtika
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
package com.playtika.test.aerospike;

import com.aerospike.client.AerospikeClient;
import com.playtika.test.common.properties.InstallPackageProperties;
import com.playtika.test.common.utils.PackageInstaller;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.GenericContainer;

import java.util.Collections;

@Configuration
@ConditionalOnBean({AerospikeClient.class, AerospikeProperties.class})
@ConditionalOnProperty(value = "embedded.aerospike.enabled", matchIfMissing = true)
public class AerospikeTestOperationsAutoConfiguration {

    @Bean
    @ConfigurationProperties("embedded.aerospike.install")
    InstallPackageProperties aerospikePackageProperties() {
        InstallPackageProperties properties = new InstallPackageProperties();
        properties.setPackages(Collections.singleton("iproute2"));// we need iproute2 for tc command to work
        return properties;
    }

    @Bean
    PackageInstaller aerospikePackageInstaller(InstallPackageProperties aerospikePackageProperties,
                                               GenericContainer aerospike) {
        return new PackageInstaller(aerospikePackageProperties, aerospike);
    }

    @Bean
    @ConditionalOnMissingBean
    public ExpiredDocumentsCleaner expiredDocumentsCleaner(AerospikeClient client,
                                                           AerospikeProperties properties) {
        return new ExpiredDocumentsCleaner(client, properties.getNamespace());
    }

    @Bean
    @ConditionalOnMissingBean
    public AerospikeTestOperations aerospikeTestOperations(ExpiredDocumentsCleaner expiredDocumentsCleaner,
                                                           GenericContainer aerospike) {
        return new AerospikeTestOperations(expiredDocumentsCleaner, aerospike);
    }

    @Bean
    @ConditionalOnMissingBean
    public AerospikeTimeTravelService aerospikeTimeTravelService(AerospikeTestOperations aerospikeTestOperations) {
        return new AerospikeTimeTravelService(aerospikeTestOperations);
    }
}
