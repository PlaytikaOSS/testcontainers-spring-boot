package com.playtika.testcontainer.clickhouse;

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
import org.springframework.util.StringUtils;
import org.testcontainers.containers.ClickHouseContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;

import java.util.LinkedHashMap;
import java.util.Optional;

import static com.playtika.testcontainer.clickhouse.ClickHouseProperties.BEAN_NAME_EMBEDDED_CLICK_HOUSE;
import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.clickhouse.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ClickHouseProperties.class)
public class EmbeddedClickHouseBootstrapConfiguration {

    private static final String CLICKHOUSE_NETWORK_ALIAS = "clickhouse.testcontainer.docker";

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "clickhouse")
    ToxiproxyClientProxy clickhouseContainerProxy(ToxiproxyClient toxiproxyClient,
                                                   ToxiproxyContainer toxiproxyContainer,
                                                   @Qualifier(BEAN_NAME_EMBEDDED_CLICK_HOUSE) ClickHouseContainer clickHouseContainer,
                                                   ClickHouseProperties properties,
                                                   ConfigurableEnvironment environment) {

        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                clickHouseContainer,
                properties.getPort(),
                "clickhouse");

        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.clickhouse", "embeddedClickhouseToxiProxyInfo", environment);

        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_CLICK_HOUSE, destroyMethod = "stop")
    public ClickHouseContainer clickHouseContainer(ClickHouseProperties properties,
                                                   ConfigurableEnvironment environment,
                                                   Optional<Network> network) {
        ClickHouseContainer clickHouseContainer = new ClickHouseContainer(ContainerUtils.getDockerImageName(properties))
                .withInitScript(properties.getInitScriptPath())
                .withNetworkAliases(CLICKHOUSE_NETWORK_ALIAS);
        network.ifPresent(clickHouseContainer::withNetwork);
        String username = !StringUtils.hasLength(properties.getUser()) ? clickHouseContainer.getUsername() : properties.getUser();
        String password = !StringUtils.hasLength(properties.getPassword()) ? clickHouseContainer.getPassword() : properties.getPassword();
        clickHouseContainer.addEnv("CLICKHOUSE_USER", username);
        clickHouseContainer.addEnv("CLICKHOUSE_PASSWORD", password == null ? "" : password);
        clickHouseContainer = (ClickHouseContainer) configureCommonsAndStart(clickHouseContainer, properties, log);
        registerClickHouseEnvironment(clickHouseContainer, environment, properties, username, password);
        return clickHouseContainer;
    }

    private void registerClickHouseEnvironment(ClickHouseContainer clickHouseContainer, ConfigurableEnvironment environment, ClickHouseProperties properties, String username, String password) {
        Integer mappedPort = clickHouseContainer.getMappedPort(properties.port);
        String host = clickHouseContainer.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.clickhouse.schema", "default");
        map.put("embedded.clickhouse.host", host);
        map.put("embedded.clickhouse.port", mappedPort);
        map.put("embedded.clickhouse.user", username);
        map.put("embedded.clickhouse.password", password);
        map.put("embedded.clickhouse.networkAlias", CLICKHOUSE_NETWORK_ALIAS);
        map.put("embedded.clickhouse.internalPort", properties.getPort());

        log.info("Started ClickHouse server. Connection details: schema=default, host={}, port={}, user={}, password={}, networkAlias={}, internalPort={}",
                host, mappedPort, username, password, CLICKHOUSE_NETWORK_ALIAS, properties.getPort());

        MapPropertySource propertySource = new MapPropertySource("embeddedClickhouseInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

}
