package com.playtika.test.couchbase;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.couchbase.config.AbstractCouchbaseConfiguration;

import java.util.List;

import static java.util.Arrays.asList;

@EnableAutoConfiguration
@Configuration
class SpringDataCouchbaseConfiguration extends AbstractCouchbaseConfiguration {

    @Value("${spring.data.couchbase.bucket}")
    String bucket;

    @Value("${spring.data.couchbase.password}")
    String password;

    @Value("${spring.data.couchbase.hosts}")
    String hosts;

    @Override
    protected List<String> getBootstrapHosts() {
        return asList(hosts.split(","));
    }

    @Override
    protected String getBucketName() {
        return bucket;
    }

    @Override
    protected String getBucketPassword() {
        return password;
    }

}
