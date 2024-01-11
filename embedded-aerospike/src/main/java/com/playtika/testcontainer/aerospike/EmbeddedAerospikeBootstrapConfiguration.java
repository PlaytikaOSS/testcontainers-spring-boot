package com.playtika.testcontainer.aerospike;

import com.aerospike.client.AerospikeClient;
import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.toxiproxy.EmbeddedToxiProxyBootstrapConfiguration;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.playtika.testcontainer.aerospike.AerospikeProperties.BEAN_NAME_AEROSPIKE;
import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;

@Slf4j
@Configuration
@ConditionalOnClass(AerospikeClient.class)
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
    ToxiproxyContainer.ContainerProxy aerospikeContainerProxy(ToxiproxyContainer toxiproxyContainer,
                                                              GenericContainer<?> aerospike,
                                                              AerospikeProperties properties,
                                                              ConfigurableEnvironment environment) {
        ToxiproxyContainer.ContainerProxy proxy = toxiproxyContainer.getProxy(aerospike, properties.port);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.aerospike.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.aerospike.toxiproxy.port", proxy.getProxyPort());
        map.put("embedded.aerospike.toxiproxy.proxyName", proxy.getName());

        MapPropertySource propertySource = new MapPropertySource("embeddedAerospikeToxiProxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        log.info("Started Aerospike ToxiProxy connection details {}", map);

        return proxy;
    }


    @Bean(name = BEAN_NAME_AEROSPIKE, destroyMethod = "stop")
    public GenericContainer<?> aerospike(AerospikeWaitStrategy aerospikeWaitStrategy,
                                      ConfigurableEnvironment environment,
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
        registerAerospikeEnvironment(aerospike, environment, properties);
        return aerospike;
    }

    private void registerAerospikeEnvironment(GenericContainer<?> aerospike,
                                              ConfigurableEnvironment environment,
                                              AerospikeProperties properties) {
        Integer mappedPort = aerospike.getMappedPort(properties.port);
        String host = aerospike.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.aerospike.host", host);
        map.put("embedded.aerospike.port", mappedPort);
        map.put("embedded.aerospike.namespace", properties.namespace);
        map.put("embedded.aerospike.networkAlias", AEROSPIKE_NETWORK_ALIAS);
        map.put("embedded.aerospike.internalPort", properties.port);

        log.info("Started aerospike server. Connection details {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedAerospikeInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
