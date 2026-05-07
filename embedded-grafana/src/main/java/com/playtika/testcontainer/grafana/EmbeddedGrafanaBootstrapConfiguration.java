package com.playtika.testcontainer.grafana;

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
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.Network;
import org.testcontainers.grafana.LgtmStackContainer;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

import java.util.LinkedHashMap;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.grafana.GrafanaProperties.GRAFANA_BEAN_NAME;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.grafana.enabled", matchIfMissing = true)
@EnableConfigurationProperties(GrafanaProperties.class)
public class EmbeddedGrafanaBootstrapConfiguration {

    private static final String GRAFANA_NETWORK_ALIAS = "grafana.testcontainer.docker";

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "grafana")
    ToxiproxyClientProxy grafanaContainerProxy(ToxiproxyClient toxiproxyClient,
                                                ToxiproxyContainer toxiproxyContainer,
                                                @Qualifier(GRAFANA_BEAN_NAME) LgtmStackContainer grafana,
                                                GrafanaProperties properties,
                                                ConfigurableEnvironment environment) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                grafana,
                properties.getPort(),
                "grafana");

        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.grafana", "embeddedGrafanaToxiproxyInfo", environment);

        return proxy;
    }

    @Bean(name = GRAFANA_BEAN_NAME, destroyMethod = "stop")
    public LgtmStackContainer grafana(ConfigurableEnvironment environment,
                                      GrafanaProperties properties,
                                      Optional<Network> network) {

        LgtmStackContainer container =
                new LgtmStackContainer(ContainerUtils.getDockerImageName(properties))
                        .withEnv("GF_SECURITY_ADMIN_USER", properties.getUsername())
                        .withEnv("GF_SECURITY_ADMIN_PASSWORD", properties.getPassword())
                        .withNetwork(Network.SHARED)
                        .withNetworkAliases(properties.getNetworkAlias(), GRAFANA_NETWORK_ALIAS);

        network.ifPresent(container::withNetwork);

        configureCommonsAndStart(container, properties, log);

        registerEnvironment(container, environment, properties);

        return container;
    }

    private void registerEnvironment(LgtmStackContainer grafana,
                                     ConfigurableEnvironment environment,
                                     GrafanaProperties properties) {

        String host = grafana.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.grafana.host", host);
        map.put("embedded.grafana.port", grafana.getMappedPort(properties.getPort()));
        map.put("embedded.grafana.username", properties.getUsername());
        map.put("embedded.grafana.password", properties.getPassword());
        map.put("embedded.grafana.networkAlias", GRAFANA_NETWORK_ALIAS);
        map.put("embedded.grafana.internalPort", properties.getPort());
        map.put("embedded.grafana.loki.port", grafana.getMappedPort(properties.getLokiPort()));
        map.put("embedded.grafana.tempo.port", grafana.getMappedPort(properties.getTempoPort()));
        map.put("embedded.grafana.otlp.grpc.port", grafana.getMappedPort(properties.getOtlpGrpcPort()));
        map.put("embedded.grafana.otlp.http.port", grafana.getMappedPort(properties.getOtlpHttpPort()));

        log.info("Started Grafana LGTM stack. Connection details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedGrafanaInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
