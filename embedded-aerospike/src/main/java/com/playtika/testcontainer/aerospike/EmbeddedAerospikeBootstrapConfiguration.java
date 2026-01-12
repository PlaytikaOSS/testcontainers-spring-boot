package com.playtika.testcontainer.aerospike;

import com.aerospike.client.IAerospikeClient;
import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.toxiproxy.EmbeddedToxiProxyBootstrapConfiguration;
import com.playtika.testcontainer.toxiproxy.ToxiproxyClientProxy;
import com.playtika.testcontainer.toxiproxy.ToxiproxyHelper;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.util.Optional;

import static com.playtika.testcontainer.aerospike.AerospikeProperties.BEAN_NAME_AEROSPIKE;
import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;

@Slf4j
@Configuration
@ConditionalOnClass(IAerospikeClient.class)
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter({DockerPresenceBootstrapConfiguration.class, EmbeddedToxiProxyBootstrapConfiguration.class})
@ConditionalOnProperty(value = "embedded.aerospike.enabled", matchIfMissing = true)
@EnableConfigurationProperties(AerospikeProperties.class)
public class EmbeddedAerospikeBootstrapConfiguration {

    private static final String AEROSPIKE_NETWORK_ALIAS = "aerospike.testcontainer.docker";

    @Bean
    @ConditionalOnMissingBean
    public AerospikeWaitStrategy aerospikeStartupCheckStrategy(AerospikeProperties properties) {
        return new AerospikeWaitStrategy(properties);
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "aerospike")
    ToxiproxyClientProxy aerospikeContainerProxy(ToxiproxyClient toxiproxyClient,
                                                  ToxiproxyContainer toxiproxyContainer,
                                                  GenericContainer<?> aerospike,
                                                  AerospikeProperties properties) {
        return ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                aerospike,
                properties.port,
                "aerospike");
    }

    @Bean
    public DynamicPropertyRegistrar aerospikeDynamicPropertyRegistrar(GenericContainer<?> aerospike, AerospikeProperties properties) {
        return registry -> {
            registry.add("embedded.aerospike.host", aerospike::getHost);
            registry.add("embedded.aerospike.port", () -> aerospike.getMappedPort(properties.port));
            registry.add("embedded.aerospike.namespace", () -> properties.namespace);
            registry.add("embedded.aerospike.networkAlias", () -> AEROSPIKE_NETWORK_ALIAS);
            registry.add("embedded.aerospike.internalPort", () -> properties.port);
        };
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "aerospike")
    public DynamicPropertyRegistrar aerospikeToxiProxyDynamicPropertyRegistrar(
        @Qualifier("aerospikeContainerProxy") ToxiproxyClientProxy proxy) {
        return ToxiproxyHelper.createToxiProxyDynamicPropertyRegistrar(proxy, "embedded.aerospike");
    }

    @Bean(name = BEAN_NAME_AEROSPIKE, destroyMethod = "stop")
    public GenericContainer<?> aerospike(AerospikeWaitStrategy aerospikeWaitStrategy,
                                      AerospikeProperties properties,
                                      Optional<Network> network) {
        WaitStrategy waitStrategy = new WaitAllStrategy()
                .withStrategy(aerospikeWaitStrategy)
                .withStrategy(new HostPortWaitStrategy())
                .withStartupTimeout(properties.getTimeoutDuration());

        GenericContainer<?> aerospike =
                new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                        .withExposedPorts(properties.port)
                        // see https://github.com/aerospike/aerospike-server.docker/blob/develop/aerospike.template.conf
                        .withEnv("NAMESPACE", properties.namespace)
                        .withEnv("SERVICE_PORT", String.valueOf(properties.port))
                        .withEnv("MEM_GB", String.valueOf(1))
                        .withEnv("STORAGE_GB", String.valueOf(1))
                        .withNetworkAliases(AEROSPIKE_NETWORK_ALIAS)
                        .waitingFor(waitStrategy);
        network.ifPresent(aerospike::withNetwork);
        String featureKey = properties.featureKey;
        if (featureKey != null) {
            // see https://github.com/aerospike/aerospike-server.docker/blob/master/template/0/aerospike.template.conf
            aerospike
                .withEnv("FEATURES", featureKey)
                .withEnv("FEATURE_KEY_FILE", "env-b64:FEATURES");
        }
        aerospike = configureCommonsAndStart(aerospike, properties, log);
        log.info("Started aerospike server. Connection details host={}, port={}, namespace={}, networkAlias={}, internalPort={}",
                aerospike.getHost(), aerospike.getMappedPort(properties.port), properties.namespace, AEROSPIKE_NETWORK_ALIAS, properties.port);
        return aerospike;
    }
}
