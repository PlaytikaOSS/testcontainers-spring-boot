package com.playtika.test.clickhouse;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.test.common.utils.ContainerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.ClickHouseContainer;
import org.testcontainers.shaded.com.google.common.base.Strings;

import java.util.LinkedHashMap;

import static com.playtika.test.clickhouse.ClickHouseProperties.BEAN_NAME_EMBEDDED_CLICK_HOUSE;
import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.clickhouse.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ClickHouseProperties.class)
public class EmbeddedClickHouseBootstrapConfiguration {

    @Bean(name = BEAN_NAME_EMBEDDED_CLICK_HOUSE, destroyMethod = "stop")
    public ClickHouseContainer clickHouseContainer(ConfigurableEnvironment environment,
                                                           ClickHouseProperties properties) {
        ClickHouseContainer clickHouseContainer = new ClickHouseContainer(ContainerUtils.getDockerImageName(properties));
        String username = Strings.isNullOrEmpty(properties.getUser()) ? clickHouseContainer.getUsername() : properties.getUser();
        String password = Strings.isNullOrEmpty(properties.getPassword()) ? clickHouseContainer.getPassword() : properties.getPassword();
        clickHouseContainer.addEnv("CLICKHOUSE_USER", username);
        clickHouseContainer.addEnv("CLICKHOUSE_PASSWORD", Strings.nullToEmpty(password));

        clickHouseContainer = (ClickHouseContainer) configureCommonsAndStart(clickHouseContainer, properties, log);

        registerClickHouseEnvironment(clickHouseContainer, environment, properties, username, password);

        return clickHouseContainer;
    }

    private void registerClickHouseEnvironment(ClickHouseContainer clickHouseContainer,
                                               ConfigurableEnvironment environment,
                                               ClickHouseProperties properties,
                                               String username, String password) {
        Integer mappedPort = clickHouseContainer.getMappedPort(properties.port);
        String host = clickHouseContainer.getContainerIpAddress();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.clickhouse.schema", "default");
        map.put("embedded.clickhouse.host", host);
        map.put("embedded.clickhouse.port", mappedPort);
        map.put("embedded.clickhouse.user", username);
        map.put("embedded.clickhouse.password", password);

        log.info("Started ClickHouse server. Connection details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedClickHouseInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

}
