package com.playtika.testcontainer.couchbase;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.couchbase.BucketDefinition;
import org.testcontainers.couchbase.CouchbaseContainer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.couchbase.CouchbaseProperties.BEAN_NAME_EMBEDDED_COUCHBASE;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.couchbase.enabled", matchIfMissing = true)
@EnableConfigurationProperties(CouchbaseProperties.class)
public class EmbeddedCouchbaseBootstrapConfiguration {

    private static final String COUCHBASE_NETWORK_ALIAS = "couchbase.testcontainer.docker";

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "couchbase")
    ToxiproxyContainer.ContainerProxy couchbaseContainerProxy(ToxiproxyContainer toxiproxyContainer,
                                                              @Qualifier(BEAN_NAME_EMBEDDED_COUCHBASE) CouchbaseContainer couchbase,
                                                              ConfigurableEnvironment environment) {
        ToxiproxyContainer.ContainerProxy proxy = toxiproxyContainer.getProxy(couchbase, couchbase.getBootstrapHttpDirectPort());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.couchbase.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.couchbase.toxiproxy.port", proxy.getProxyPort());
        map.put("embedded.couchbase.toxiproxy.proxyName", proxy.getName());

        MapPropertySource propertySource = new MapPropertySource("embeddedCouchbaseToxiProxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        log.info("Started Couchbase ToxiProxy connection details {}", map);

        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_COUCHBASE, destroyMethod = "stop")
    public CouchbaseContainer couchbase(ConfigurableEnvironment environment,
                                        CouchbaseProperties properties,
                                        Optional<Network> network) {
        BucketDefinition bucketDefinition = new BucketDefinition(properties.getBucket())
                .withPrimaryIndex(true)
                .withQuota(properties.getBucketRamMb());

        CouchbaseContainer couchbase = new CouchbaseContainer(ContainerUtils.getDockerImageName(properties))
                .withBucket(bucketDefinition)
                .withEnabledServices(properties.getServices())
                .withCredentials(properties.getUser(), properties.getPassword())
                .withNetworkAliases(COUCHBASE_NETWORK_ALIAS);

        network.ifPresent(couchbase::withNetwork);
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
        map.put("embedded.couchbase.networkAlias", COUCHBASE_NETWORK_ALIAS);

        log.info("Started couchbase server. Connection details {},  " +
                        "Admin UI: http://localhost:{}, user: {}, password: {}",
                map, mappedHttpPort, properties.getUser(), properties.getPassword());

        MapPropertySource propertySource = new MapPropertySource("embeddedCouchbaseInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
