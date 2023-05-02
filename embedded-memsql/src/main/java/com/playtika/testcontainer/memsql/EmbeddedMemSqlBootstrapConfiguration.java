package com.playtika.testcontainer.memsql;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
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
import java.util.Map;
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

    @Bean
    @ConditionalOnMissingBean
    MemSqlStatusCheck memSqlStartupCheckStrategy(MemSqlProperties properties) {
        return new MemSqlStatusCheck(properties);
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "memsql")
    ToxiproxyContainer.ContainerProxy memsqlContainerProxy(ToxiproxyContainer toxiproxyContainer,
                                                          @Qualifier(BEAN_NAME_EMBEDDED_MEMSQL) GenericContainer<?> memsql,
                                                          MemSqlProperties properties,
                                                          ConfigurableEnvironment environment) {
        ToxiproxyContainer.ContainerProxy proxy = toxiproxyContainer.getProxy(memsql, properties.getPort());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.memsql.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.memsql.toxiproxy.port", proxy.getProxyPort());
        map.put("embedded.memsql.toxiproxy.proxyName", proxy.getName());

        MapPropertySource propertySource = new MapPropertySource("embeddedMemsqlToxiproxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        log.info("Started Memsql ToxiProxy connection details {}", map);

        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_MEMSQL, destroyMethod = "stop")
    public GenericContainer<?> memsql(ConfigurableEnvironment environment,
                                      MemSqlProperties properties,
                                      MemSqlStatusCheck memSqlStatusCheck,
                                      Optional<Network> network) {
        GenericContainer<?> memsql = new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                .withEnv("IGNORE_MIN_REQUIREMENTS", "1")
                .withEnv("LICENSE_KEY", properties.getLicenseKey())
                .withEnv("SINGLESTORE_LICENSE", properties.getLicenseKey())
                .withEnv("ROOT_PASSWORD", properties.getPassword())
                .withEnv("START_AFTER_INIT", "Y")
                .withExposedPorts(properties.port)
                .withCopyFileToContainer(MountableFile.forClasspathResource("mem.sql"), "/schema.sql")
                .waitingFor(memSqlStatusCheck);
        if ("aarch".equals(System.getProperty("system.arch"))){
            memsql = memsql.withCommand("platform", "linux/amd64");
        }
        network.ifPresent(memsql::withNetwork);
        memsql = configureCommonsAndStart(memsql, properties, log);
        registerMemSqlEnvironment(memsql, environment, properties);
        return memsql;
    }

    private void registerMemSqlEnvironment(GenericContainer<?> memsql,
                                           ConfigurableEnvironment environment,
                                           MemSqlProperties properties) {
        Integer mappedPort = memsql.getMappedPort(properties.port);
        String host = memsql.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.memsql.port", mappedPort);
        map.put("embedded.memsql.host", host);
        map.put("embedded.memsql.schema", properties.getDatabase());
        map.put("embedded.memsql.user", properties.getUser());
        map.put("embedded.memsql.password", properties.getPassword());

        log.info("Started memsql server. Connection details {} ", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedMemSqlInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
