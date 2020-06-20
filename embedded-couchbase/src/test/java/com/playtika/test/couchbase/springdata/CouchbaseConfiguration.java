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
package com.playtika.test.couchbase.springdata;

import com.couchbase.client.core.env.SeedNode;
import com.couchbase.client.core.env.TimeoutConfig;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.ClusterOptions;
import com.couchbase.client.java.env.ClusterEnvironment;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.couchbase.config.AbstractCouchbaseConfiguration;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;


@Configuration
@AllArgsConstructor
@EnableAutoConfiguration
@EnableConfigurationProperties(CouchbaseConfigurationProperties.class)
public class CouchbaseConfiguration extends AbstractCouchbaseConfiguration {

    private final CouchbaseConfigurationProperties couchbaseConfigurationProperties;

    @Override
    public String getConnectionString() {
        return "couchbase://" + couchbaseConfigurationProperties.getHosts();
    }

    @Override
    protected void configureEnvironment(ClusterEnvironment.Builder builder) {
        builder.timeoutConfig(TimeoutConfig.kvTimeout(Duration.ofSeconds(20)));
        super.configureEnvironment(builder);
    }

    @Primary
    @Bean(destroyMethod = "disconnect")
    @Override
    public Cluster couchbaseCluster(ClusterEnvironment couchbaseClusterEnvironment) {
        Set<SeedNode> seedNodes = new HashSet<>(Collections.singletonList(
                SeedNode.create(couchbaseConfigurationProperties.getHosts(),
                        Optional.of(couchbaseConfigurationProperties.getBootstrapCarrierDirectPort()),
                        Optional.of(couchbaseConfigurationProperties.getBootstrapHttpDirectPort()))));

        ClusterOptions options = ClusterOptions
                .clusterOptions(couchbaseConfigurationProperties.getUser(), couchbaseConfigurationProperties.getPassword())
                .environment(couchbaseClusterEnvironment);

        return Cluster.connect(seedNodes, options);
    }

    @Override
    public String getUserName() {
        return couchbaseConfigurationProperties.getUser();
    }

    @Override
    public String getPassword() {
        return couchbaseConfigurationProperties.getPassword();
    }

    @Override
    public String getBucketName() {
        return couchbaseConfigurationProperties.getBucket();
    }
}
