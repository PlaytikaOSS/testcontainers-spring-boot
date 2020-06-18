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
package com.playtika.test.couchbase;

import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.playtika.test.common.spring.DependsOnPostProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.playtika.test.couchbase.CouchbaseProperties.BEAN_NAME_EMBEDDED_COUCHBASE;

@Slf4j
@Configuration
@AutoConfigureOrder
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(name = "embedded.couchbase.enabled", matchIfMissing = true)
@AutoConfigureAfter(name = "org.springframework.boot.autoconfigure.data.couchbase.CouchbaseDataAutoConfiguration")
public class EmbeddedCouchbaseDependenciesAutoConfiguration {

    @Configuration
    @ConditionalOnClass(Bucket.class)
    public static class CouchbaseBucketDependencyContext {

        @Bean
        public static BeanFactoryPostProcessor bucketDependencyPostProcessor() {
            return new DependsOnPostProcessor(Bucket.class, new String[]{BEAN_NAME_EMBEDDED_COUCHBASE});
        }
    }

    @Configuration
    @ConditionalOnClass(AsyncBucket.class)
    public static class CouchbaseAsyncBucketDependencyContext {

        @Bean
        public static BeanFactoryPostProcessor asyncBucketDependencyPostProcessor() {
            return new DependsOnPostProcessor(AsyncBucket.class, new String[]{BEAN_NAME_EMBEDDED_COUCHBASE});
        }
    }

    @Configuration
    @ConditionalOnClass(CouchbaseClient.class)
    public static class CouchbaseClientDependencyContext {

        @Bean
        public static BeanFactoryPostProcessor couchbaseClientDependencyPostProcessor() {
            return new DependsOnPostProcessor(CouchbaseClient.class, new String[]{BEAN_NAME_EMBEDDED_COUCHBASE});
        }
    }


    @Configuration
    @ConditionalOnClass(Cluster.class)
    public static class CouchbaseClusterDependencyContext {

        @Bean
        public static BeanFactoryPostProcessor couchbaseClusterDependencyPostProcessor() {
            return new DependsOnPostProcessor(Cluster.class, new String[]{BEAN_NAME_EMBEDDED_COUCHBASE});
        }
    }

}
