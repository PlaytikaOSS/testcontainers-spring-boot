package com.playtika.testcontainer.couchbase;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.toxiproxy.ToxiproxyClientProxy;
import com.playtika.testcontainer.toxiproxy.ToxiproxyHelper;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.Network;
import org.testcontainers.couchbase.BucketDefinition;
import org.testcontainers.couchbase.CouchbaseContainer;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.couchbase.CouchbaseProperties.BEAN_NAME_EMBEDDED_COUCHBASE;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@AutoConfigureBefore(name = {
        "org.springframework.boot.autoconfigure.couchbase.CouchbaseAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.couchbase.CouchbaseDataAutoConfiguration"
})
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "embedded.couchbase.enabled", matchIfMissing = true)
@EnableConfigurationProperties(CouchbaseProperties.class)
public class EmbeddedCouchbaseBootstrapConfiguration {

    private static final String COUCHBASE_NETWORK_ALIAS = "couchbase.testcontainer.docker";

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "couchbase")
    ToxiproxyClientProxy couchbaseContainerProxy(ToxiproxyClient toxiproxyClient,
                                                  ToxiproxyContainer toxiproxyContainer,
                                                  @Qualifier(BEAN_NAME_EMBEDDED_COUCHBASE) CouchbaseContainer couchbase) {
        return ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                couchbase,
                couchbase.getBootstrapHttpDirectPort(),
                "couchbase");
    }

    @Bean(name = BEAN_NAME_EMBEDDED_COUCHBASE, destroyMethod = "stop")
    public CouchbaseContainer couchbase(CouchbaseProperties properties,
                                        Optional<Network> network) {
        BucketDefinition bucketDefinition = new BucketDefinition(properties.getBucket())
                .withPrimaryIndex(false)
                .withQuota(properties.getBucketRamMb());

        CouchbaseContainer couchbase = new CouchbaseContainer(ContainerUtils.getDockerImageName(properties))
                .withBucket(bucketDefinition)
                .withEnabledServices(properties.getServices())
                .withCredentials(properties.getUser(), properties.getPassword())
                .withNetworkAliases(COUCHBASE_NETWORK_ALIAS);

        network.ifPresent(couchbase::withNetwork);
        couchbase = (CouchbaseContainer) configureCommonsAndStart(couchbase, properties, log);
        return couchbase;
    }

    @Bean
    public DynamicPropertyRegistrar couchbaseDynamicPropertyRegistrar(
            @Qualifier(BEAN_NAME_EMBEDDED_COUCHBASE) CouchbaseContainer couchbase,
            CouchbaseProperties properties) {
        return registry -> {
            Integer mappedHttpPort = couchbase.getBootstrapHttpDirectPort();
            Integer mappedCarrierPort = couchbase.getBootstrapCarrierDirectPort();
            String host = couchbase.getHost();

            // System properties must be set before client initialization
            // These are required by Couchbase SDK for auto-discovery
            System.setProperty("com.couchbase.bootstrapHttpDirectPort", String.valueOf(mappedHttpPort));
            System.setProperty("com.couchbase.bootstrapCarrierDirectPort", String.valueOf(mappedCarrierPort));

            registry.add("embedded.couchbase.bootstrapHttpDirectPort", () -> mappedHttpPort);
            registry.add("embedded.couchbase.bootstrapCarrierDirectPort", () -> mappedCarrierPort);
            registry.add("embedded.couchbase.host", () -> host);
            registry.add("embedded.couchbase.bucket", properties::getBucket);
            registry.add("embedded.couchbase.user", properties::getUser);
            registry.add("embedded.couchbase.password", properties::getPassword);
            registry.add("embedded.couchbase.networkAlias", () -> COUCHBASE_NETWORK_ALIAS);

            log.info("Started couchbase server. Connection details: host={}, httpPort={}, carrierPort={}, user={}, password={}, " +
                            "Admin UI: http://localhost:{}",
                    host, mappedHttpPort, mappedCarrierPort, properties.getUser(), properties.getPassword(), mappedHttpPort);
        };
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "couchbase")
    public DynamicPropertyRegistrar couchbaseToxiProxyDynamicPropertyRegistrar(
            @Qualifier("couchbaseContainerProxy") ToxiproxyClientProxy proxy) {
        return ToxiproxyHelper.createToxiProxyDynamicPropertyRegistrar(proxy, "embedded.couchbase");
    }
}
