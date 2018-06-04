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
package com.playtika.test.couchbase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;

import java.util.LinkedHashMap;

import static com.playtika.test.couchbase.CouchbaseProperties.BEAN_NAME_EMBEDDED_COUCHBASE;

@Slf4j
@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "embedded.couchbase.enabled", matchIfMissing = true)
@EnableConfigurationProperties(CouchbaseProperties.class)
public class EmbeddedCouchbaseBootstrapConfiguration {

    @Bean(name = BEAN_NAME_EMBEDDED_COUCHBASE, destroyMethod = "stop")
    public GenericContainer couchbase(ConfigurableEnvironment environment,
                                      CouchbaseProperties properties) throws Exception {

        log.info("Starting couchbase server. Docker image: {}", properties.dockerImage);
        GenericContainer couchbase = CouchbaseContainerFactory.create(properties, log);
        couchbase.start();
        registerCouchbaseEnvironment(couchbase, environment, properties);
        return couchbase;
    }

    private void registerCouchbaseEnvironment(GenericContainer couchbase,
                                              ConfigurableEnvironment environment,
                                              CouchbaseProperties properties) {

        Integer mappedHttpPort = couchbase.getMappedPort(properties.httpDirectPort);
        Integer mappedCarrierPort = couchbase.getMappedPort(properties.carrierDirectPort);
        String host = couchbase.getContainerIpAddress();

        System.setProperty("com.couchbase.bootstrapHttpDirectPort", String.valueOf(mappedHttpPort));
        System.setProperty("com.couchbase.bootstrapCarrierDirectPort", String.valueOf(mappedCarrierPort));

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.couchbase.bootstrapHttpDirectPort", mappedHttpPort);
        map.put("embedded.couchbase.bootstrapCarrierDirectPort", mappedCarrierPort);
        map.put("embedded.couchbase.host", host);
        map.put("embedded.couchbase.bucket", properties.bucket);
        map.put("embedded.couchbase.user", properties.bucket);
        map.put("embedded.couchbase.password", properties.password);

        log.info("Started couchbase server. Connection details {},  " +
                        "Admin UI: http://localhost:{}, user: {}, password: {}",
                map, mappedHttpPort, properties.getUser(), properties.getPassword());

        MapPropertySource propertySource = new MapPropertySource("embeddedCouchbaseInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
