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
package com.playtika.test.minio;

import com.playtika.test.common.properties.InstallPackageProperties;
import com.playtika.test.common.utils.MicroDnfPackageInstaller;
import com.playtika.test.common.utils.PackageInstaller;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.GenericContainer;

import java.util.Collections;

import static com.playtika.test.minio.MinioProperties.MINIO_BEAN_NAME;

@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnBean({MinioProperties.class})
@ConditionalOnProperty(value = "embedded.minio.enabled", matchIfMissing = true)
public class EmbeddedMinioTestOperationsAutoConfiguration {

    @Bean
    @ConfigurationProperties("embedded.minio.install")
    InstallPackageProperties minioPackageProperties() {
        InstallPackageProperties properties = new InstallPackageProperties();
//        properties.setPackages(Collections.singleton("iproute2"));// we need iproute2 for tc command to work
        return properties;
    }

    @Bean
    PackageInstaller minioPackageInstaller(
            InstallPackageProperties minioPackageProperties,
            @Qualifier(MINIO_BEAN_NAME) GenericContainer minio
    ) {
        return new MicroDnfPackageInstaller(minioPackageProperties, minio);
    }

// Current image doesn't support `tc` command, since Minio is currently based on ubi-minimal with microdnf package manager. `iproute2` package is not available here.
// This bean is commented, so that users that expect NetworkTestOperations in the tests are notified that this is not supported anymore.
//    @Bean
//    @ConditionalOnMissingBean(name = "minioNetworkTestOperations")
//    public NetworkTestOperations minioNetworkTestOperations(
//            @Qualifier(MINIO_BEAN_NAME) GenericContainer minio
//    ) {
//        return new DefaultNetworkTestOperations(minio);
//    }
}
