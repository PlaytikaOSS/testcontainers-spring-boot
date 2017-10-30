package com.playtika.test.couchbase;

import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.CouchbaseConnectionFactoryBuilder;
import net.spy.memcached.ConnectionFactoryBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class LegacyClientConfiguration {

    @Value("${spring.data.couchbase.bucket}")
    String bucket;

    @Value("${spring.data.couchbase.password}")
    String password;

    @Value("${spring.data.couchbase.hosts}")
    String hosts;

    @Value("${spring.data.couchbase.bootstrapHttpPort}")
    int bootstrapHttpPort;

    @Value("${spring.data.couchbase.carrierHttpPort}")
    int carrierHttpPort;

    @Bean(destroyMethod = "shutdown")
    public CouchbaseClient testCouchbaseClient() throws Exception {

        List<String> hostsList = Arrays.asList(hosts.split(","));
        List<URI> uris = toUris(hostsList);

        CouchbaseConnectionFactoryBuilder factoryBuilder = new CouchbaseConnectionFactoryBuilder();
        factoryBuilder.setProtocol(ConnectionFactoryBuilder.Protocol.BINARY);
        factoryBuilder.setOpTimeout(4_000);

        return new CouchbaseClient(factoryBuilder.buildCouchbaseConnection(
                uris,
                bucket,
                password
        ));
    }

    private List<URI> toUris(List<String> hosts) throws URISyntaxException {
        List<URI> uris = new ArrayList<>();
        for (String host : hosts) {
            uris.add(new URI(String.format("http://%s:%d/pools", host, bootstrapHttpPort)));
        }
        return uris;
    }
}
