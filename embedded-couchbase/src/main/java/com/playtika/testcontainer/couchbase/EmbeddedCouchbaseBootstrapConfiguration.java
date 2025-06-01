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
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.couchbase.BucketDefinition;
import org.testcontainers.couchbase.CouchbaseContainer;

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
    ToxiproxyClientProxy couchbaseContainerProxy(ToxiproxyClient toxiproxyClient,
                                                  ToxiproxyContainer toxiproxyContainer,
                                                  @Qualifier(BEAN_NAME_EMBEDDED_COUCHBASE) CouchbaseContainer couchbase,
                                                  ConfigurableEnvironment environment) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                couchbase,
                couchbase.getBootstrapHttpDirectPort(),
                "couchbase");

        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.couchbase", "embeddedCouchbaseToxiProxyInfo", environment);

        return proxy;
    ToxiproxyContainer.ContainerProxy couchbaseContainerProxy(ToxiproxyContainer toxiproxyContainer,
                                                              @Qualifier(BEAN_NAME_EMBEDDED_COUCHBASE) CouchbaseContainer couchbase) {
        return toxiproxyContainer.getProxy(couchbase, couchbase.getBootstrapHttpDirectPort());
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "couchbase")
    public DynamicPropertyRegistrar couchbaseToxiProxyDynamicPropertyRegistrar(
        @Qualifier("couchbaseContainerProxy") ToxiproxyContainer.ContainerProxy proxy) {
        return registry -> {
            registry.add("embedded.couchbase.toxiproxy.host", proxy::getContainerIpAddress);
            registry.add("embedded.couchbase.toxiproxy.port", proxy::getProxyPort);
            registry.add("embedded.couchbase.toxiproxy.proxyName", proxy::getName);
            log.info("Started Couchbase ToxiProxy connection details host={}, port={}, proxyName={}", proxy.getContainerIpAddress(), proxy.getProxyPort(), proxy.getName());
        };
    }

    @Bean(name = BEAN_NAME_EMBEDDED_COUCHBASE, destroyMethod = "stop")
    public CouchbaseContainer couchbase(CouchbaseProperties properties, Optional<Network> network) {
        BucketDefinition bucketDefinition = new BucketDefinition(properties.getBucket())
                .withPrimaryIndex(true)
                .withQuota(properties.getBucketRamMb());

        CouchbaseContainer couchbase = new CouchbaseContainer(ContainerUtils.getDockerImageName(properties))
                .withBucket(bucketDefinition)
                .withEnabledServices(properties.getServices())
                .withCredentials(properties.getUser(), properties.getPassword())
                .withNetworkAliases(COUCHBASE_NETWORK_ALIAS);

        network.ifPresent(couchbase::withNetwork);
        configureCommonsAndStart(couchbase, properties, log);
        return couchbase;
    }

    @Bean
    public DynamicPropertyRegistrar couchbaseDynamicPropertyRegistrar(
        @Qualifier(BEAN_NAME_EMBEDDED_COUCHBASE) CouchbaseContainer couchbase,
        CouchbaseProperties properties) {
        return registry -> {
            registry.add("embedded.couchbase.host", couchbase::getHost);
            registry.add("embedded.couchbase.bootstrapHttpDirectPort", couchbase::getBootstrapHttpDirectPort);
            registry.add("embedded.couchbase.bootstrapCarrierDirectPort", couchbase::getBootstrapCarrierDirectPort);
            registry.add("embedded.couchbase.networkAlias", () -> COUCHBASE_NETWORK_ALIAS);

            registry.add("embedded.couchbase.bucket", () -> properties.bucket);
            registry.add("embedded.couchbase.user", () -> properties.user);
            registry.add("embedded.couchbase.password", () -> properties.password);
        };
    }
}
