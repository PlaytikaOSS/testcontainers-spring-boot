package com.playtika.testcontainer.memsql;

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
import org.testcontainers.utility.MountableFile;

import java.util.LinkedHashMap;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.memsql.MemSqlProperties.BEAN_NAME_EMBEDDED_MEMSQL;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.memsql.enabled", matchIfMissing = true)
@EnableConfigurationProperties(MemSqlProperties.class)
public class EmbeddedMemSqlBootstrapConfiguration {

    private static final String MEMSQL_NETWORK_ALIAS = "memsql.testcontainer.docker";

    @Bean
    @ConditionalOnMissingBean
    MemSqlStatusCheck memSqlStartupCheckStrategy(MemSqlProperties properties) {
        return new MemSqlStatusCheck(properties);
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "memsql")
    ToxiproxyClientProxy memsqlContainerProxy(ToxiproxyClient toxiproxyClient,
                                               ToxiproxyContainer toxiproxyContainer,
                                               @Qualifier(BEAN_NAME_EMBEDDED_MEMSQL) GenericContainer<?> memsql,
                                               MemSqlProperties properties,
                                               ConfigurableEnvironment environment) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                memsql,
                properties.getPort(),
                "memsql");

        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.memsql", "embeddedMemsqlToxiProxyInfo", environment);

        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_MEMSQL, destroyMethod = "stop")
    public GenericContainer<?> memsql(MemSqlProperties properties,
                                      MemSqlStatusCheck memSqlStatusCheck,
                                      ConfigurableEnvironment environment,
                                      Optional<Network> network) {
        GenericContainer<?> memsql = new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                .withEnv("IGNORE_MIN_REQUIREMENTS", "1")
                .withEnv("SINGLESTORE_SET_GLOBAL_DEFAULT_PARTITIONS_PER_LEAF", "1")
                .withEnv("LICENSE_KEY", properties.getLicenseKey())
                .withEnv("SINGLESTORE_LICENSE", properties.getLicenseKey())
                .withEnv("ROOT_PASSWORD", properties.getPassword())
                .withEnv("START_AFTER_INIT", "Y")
                .withExposedPorts(properties.port)
                .withCopyFileToContainer(MountableFile.forClasspathResource("mem.sql"), "/schema.sql")
                .waitingFor(memSqlStatusCheck)
                .withNetworkAliases(MEMSQL_NETWORK_ALIAS);
        if ("aarch".equals(System.getProperty("system.arch"))){
            memsql = memsql.withCommand("platform", "linux/amd64");
        }
        network.ifPresent(memsql::withNetwork);
        memsql = configureCommonsAndStart(memsql, properties, log);
        registerMemsqlEnvironment(memsql, environment, properties);
        return memsql;
    }

    private void registerMemsqlEnvironment(GenericContainer<?> memsql, ConfigurableEnvironment environment, MemSqlProperties properties) {
        Integer mappedPort = memsql.getMappedPort(properties.port);
        String host = memsql.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.memsql.port", mappedPort);
        map.put("embedded.memsql.host", host);
        map.put("embedded.memsql.schema", properties.getDatabase());
        map.put("embedded.memsql.user", properties.getUser());
        map.put("embedded.memsql.password", properties.getPassword());
        map.put("embedded.memsql.networkAlias", MEMSQL_NETWORK_ALIAS);
        map.put("embedded.memsql.internalPort", properties.getPort());

        MapPropertySource propertySource = new MapPropertySource("embeddedMemsqlInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
