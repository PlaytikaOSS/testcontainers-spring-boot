/*
* The MIT License (MIT)
*
* Copyright (c) 2017 Playtika
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
package com.playtika.test.couchbase;

import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.Bucket;
import com.playtika.test.common.checks.AbstractInitOnStartupStrategy;
import com.playtika.test.common.checks.CompositeStartupCheckStrategy;
import com.playtika.test.common.spring.DependsOnPostProcessor;
import com.playtika.test.couchbase.rest.CreateBucket;
import com.playtika.test.couchbase.rest.CreateBucketUser;
import com.playtika.test.couchbase.rest.SetupRamQuotas;
import com.playtika.test.couchbase.rest.SetupAdminUserAndPassword;
import com.playtika.test.couchbase.rest.SetupIndexesType;
import com.playtika.test.couchbase.rest.SetupNodeStorage;
import com.playtika.test.couchbase.rest.SetupServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.StartupCheckStrategy;

import java.util.LinkedHashMap;
import java.util.List;

import static com.playtika.test.common.utils.ContainerUtils.containerLogsConsumer;
import static com.playtika.test.couchbase.CouchbaseProperties.BEAN_NAME_EMBEDDED_COUCHBASE;
import static java.util.Arrays.asList;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@Slf4j
@Configuration
@Order(HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "embedded.couchbase.enabled", matchIfMissing = true)
@EnableConfigurationProperties(CouchbaseProperties.class)
public class EmbeddedCouchbaseAutoConfiguration {

    @Bean(name = BEAN_NAME_EMBEDDED_COUCHBASE, destroyMethod = "stop")
    public GenericContainer couchbase(ConfigurableEnvironment environment,
                                      CouchbaseProperties properties) throws Exception {
        CompositeStartupCheckStrategy compositeStartupCheck = getCompositeStartupCheckStrategy(properties);
        GenericContainer couchbase = new GenericContainer(properties.dockerImage)
                .withStartupCheckStrategy(compositeStartupCheck)
                .withLogConsumer(containerLogsConsumer(log))
                .withExposedPorts(
                        properties.httpDirectPort
                        , properties.carrierDirectPort
                );
        couchbase.start();
        registerCouchbaseEnvironment(couchbase, environment, properties);
        return couchbase;
    }

    /**
     * https://developer.couchbase.com/documentation/server/current/rest-api/rest-node-provisioning.html
     */
    private CompositeStartupCheckStrategy getCompositeStartupCheckStrategy(CouchbaseProperties properties) {
        List<StartupCheckStrategy> strategies = asList(new AbstractInitOnStartupStrategy[]{
                new SetupNodeStorage(properties),
                new SetupRamQuotas(properties),
                new SetupServices(properties),
                new SetupIndexesType(properties),
                new SetupAdminUserAndPassword(properties),
                new CreateBucket(properties),
                new CreateBucketUser(properties),
        });
        return CompositeStartupCheckStrategy.builder()
                .checksToPerform(strategies)
                .build();
    }

    private void registerCouchbaseEnvironment(GenericContainer couchbase,
                                              ConfigurableEnvironment environment,
                                              CouchbaseProperties properties) {

        Integer mappedHttpPort = couchbase.getMappedPort(properties.httpDirectPort);
        Integer mappedCarrierPort = couchbase.getMappedPort(properties.carrierDirectPort);
        String host = couchbase.getContainerIpAddress();

        log.info("Started couchbase server. Admin UI: http://localhost:{}, user: {}, password: {}"
                , mappedHttpPort, properties.getUser(), properties.getPassword());

        System.setProperty("com.couchbase.bootstrapHttpDirectPort", String.valueOf(mappedHttpPort));
        System.setProperty("com.couchbase.bootstrapCarrierDirectPort", String.valueOf(mappedCarrierPort));

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.couchbase.bootstrapHttpDirectPort", mappedHttpPort);
        map.put("embedded.couchbase.bootstrapCarrierDirectPort", mappedCarrierPort);
        map.put("embedded.couchbase.host", host);
        map.put("embedded.couchbase.bucket", properties.bucket);
        map.put("embedded.couchbase.user", properties.bucket);
        map.put("embedded.couchbase.password", properties.password);
        MapPropertySource propertySource = new MapPropertySource("embeddedCouchbaseInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

    @Configuration
    @ConditionalOnClass(Bucket.class)
    public static class CouchbaseBucketDependencyContext {

        @Bean
        public BeanFactoryPostProcessor bucketDependencyPostProcessor() {
            return new DependsOnPostProcessor(Bucket.class, new String[]{BEAN_NAME_EMBEDDED_COUCHBASE});
        }
    }

    @Configuration
    @ConditionalOnClass(AsyncBucket.class)
    public static class CouchbaseAsyncBucketDependencyContext {

        @Bean
        public BeanFactoryPostProcessor asyncBucketDependencyPostProcessor() {
            return new DependsOnPostProcessor(AsyncBucket.class, new String[]{BEAN_NAME_EMBEDDED_COUCHBASE});
        }
    }

    @Configuration
    @ConditionalOnClass(CouchbaseClient.class)
    public static class CouchbaseClientDependencyContext {

        @Bean
        public BeanFactoryPostProcessor couchbaseClientDependencyPostProcessor() {
            return new DependsOnPostProcessor(CouchbaseClient.class, new String[]{BEAN_NAME_EMBEDDED_COUCHBASE});
        }
    }
}
