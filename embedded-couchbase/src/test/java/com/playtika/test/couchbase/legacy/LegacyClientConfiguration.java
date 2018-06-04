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
package com.playtika.test.couchbase.legacy;

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

    @Value("${couchbase.bucket}")
    String bucket;

    @Value("${couchbase.password}")
    String password;

    @Value("${couchbase.hosts}")
    String hosts;

    @Value("${couchbase.bootstrapHttpDirectPort}")
    int bootstrapHttpPort;

    @Value("${couchbase.bootstrapCarrierDirectPort}")
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
