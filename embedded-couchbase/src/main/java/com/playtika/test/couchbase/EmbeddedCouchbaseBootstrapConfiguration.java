package com.playtika.test.couchbase;

import com.github.dockerjava.api.model.Capability;
import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.couchbase.BucketDefinition;
import org.testcontainers.couchbase.CouchbaseContainer;

import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.test.couchbase.CouchbaseProperties.BEAN_NAME_EMBEDDED_COUCHBASE;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.couchbase.enabled", matchIfMissing = true)
@EnableConfigurationProperties(CouchbaseProperties.class)
public class EmbeddedCouchbaseBootstrapConfiguration {

    @Bean(name = BEAN_NAME_EMBEDDED_COUCHBASE, destroyMethod = "stop")
    public CouchbaseContainer couchbase(ConfigurableEnvironment environment,
                                        CouchbaseProperties properties) {

        log.info("Starting couchbase server. Docker image: {}", properties.dockerImage);
        BucketDefinition bucketDefinition = new BucketDefinition(properties.getBucket())
                .withPrimaryIndex(true)
                .withQuota(properties.getBucketRamMb());

        CouchbaseContainer couchbase = new CouchbaseContainer(properties.dockerImage)
                .withBucket(bucketDefinition)
                .withEnabledServices(properties.getServices())
                .withCredentials(properties.getUser(), properties.getPassword());

        couchbase = (CouchbaseContainer) configureCommonsAndStart(couchbase, properties, log);

        registerCouchbaseEnvironment(couchbase, environment, properties);
        return couchbase;
    }

    private void registerCouchbaseEnvironment(CouchbaseContainer couchbase,
                                              ConfigurableEnvironment environment,
                                              CouchbaseProperties properties) {

        Integer mappedHttpPort = couchbase.getBootstrapHttpDirectPort();
        Integer mappedCarrierPort = couchbase.getBootstrapCarrierDirectPort();
        String host = couchbase.getHost();

        System.setProperty("com.couchbase.bootstrapHttpDirectPort", String.valueOf(mappedHttpPort));
        System.setProperty("com.couchbase.bootstrapCarrierDirectPort", String.valueOf(mappedCarrierPort));

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.couchbase.bootstrapHttpDirectPort", mappedHttpPort);
        map.put("embedded.couchbase.bootstrapCarrierDirectPort", mappedCarrierPort);
        map.put("embedded.couchbase.host", host);
        map.put("embedded.couchbase.bucket", properties.bucket);
        map.put("embedded.couchbase.user", properties.user);
        map.put("embedded.couchbase.password", properties.password);

        log.info("Started couchbase server. Connection details {},  " +
                        "Admin UI: http://localhost:{}, user: {}, password: {}",
                map, mappedHttpPort, properties.getUser(), properties.getPassword());

        MapPropertySource propertySource = new MapPropertySource("embeddedCouchbaseInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
