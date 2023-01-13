package com.playtika.test.memsql;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.test.common.utils.ContainerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.testcontainers.utility.MountableFile;

import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.test.memsql.MemSqlProperties.BEAN_NAME_EMBEDDED_MEMSQL;

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

    @Bean(name = BEAN_NAME_EMBEDDED_MEMSQL, destroyMethod = "stop")
    public GenericContainer<?> memsql(ConfigurableEnvironment environment,
                                   MemSqlProperties properties,
                                   MemSqlStatusCheck memSqlStatusCheck,
                                   @Autowired(required = false) Network network) {

        GenericContainer<?> memsql = new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                .withEnv("IGNORE_MIN_REQUIREMENTS", "1")
                .withEnv("LICENSE_KEY", properties.getLicenseKey())
                .withEnv("SINGLESTORE_LICENSE", properties.getLicenseKey())
                .withEnv("ROOT_PASSWORD", properties.getPassword())
                .withEnv("START_AFTER_INIT", "Y")
                .withExposedPorts(properties.port)
                .withCopyFileToContainer(MountableFile.forClasspathResource("mem.sql"), "/schema.sql")
                .waitingFor(memSqlStatusCheck);
        if (network != null) {
          memsql = memsql.withNetwork(network);
        }
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
