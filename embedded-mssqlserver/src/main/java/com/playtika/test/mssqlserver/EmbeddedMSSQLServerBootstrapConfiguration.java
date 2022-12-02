package com.playtika.test.mssqlserver;

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
import org.springframework.util.StringUtils;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.mssqlserver.enabled", matchIfMissing = true)
@EnableConfigurationProperties(MSSQLServerProperties.class)
public class EmbeddedMSSQLServerBootstrapConfiguration {

    @Bean(name = MSSQLServerProperties.BEAN_NAME_EMBEDDED_MSSQLSERVER, destroyMethod = "stop")
    public EmbeddedMSSQLServerContainer mssqlserver(ConfigurableEnvironment environment,
                                    MSSQLServerProperties properties) {

        EmbeddedMSSQLServerContainer mssqlServerContainer = new EmbeddedMSSQLServerContainer(ContainerUtils.getDockerImageName(properties))
                .withPassword(properties.getPassword())
                .withInitScript(properties.getInitScriptPath());

        String startupLogCheckRegex = properties.getStartupLogCheckRegex();
        if (StringUtils.hasLength(startupLogCheckRegex)) {
            WaitStrategy waitStrategy = new LogMessageWaitStrategy()
                    .withRegEx(startupLogCheckRegex);
            mssqlServerContainer.setWaitStrategy(waitStrategy);
        }

        if (properties.isAcceptLicence()) {
            mssqlServerContainer.acceptLicense();
        }

        mssqlServerContainer = (EmbeddedMSSQLServerContainer) configureCommonsAndStart(mssqlServerContainer, properties, log);
        registerMSSQLServerEnvironment(mssqlServerContainer, environment, properties);

        return mssqlServerContainer;
    }

    private void registerMSSQLServerEnvironment(MSSQLServerContainer<?> mssqlServerContainer,
                                        ConfigurableEnvironment environment,
                                        MSSQLServerProperties properties) {
        Integer mappedPort = mssqlServerContainer.getMappedPort(MSSQLServerContainer.MS_SQL_SERVER_PORT);
        String host = mssqlServerContainer.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.mssqlserver.port", mappedPort);
        map.put("embedded.mssqlserver.host", host);
        // Database and user cannot be chosen when starting the MSSQL image
        map.put("embedded.mssqlserver.database", "master");
        map.put("embedded.mssqlserver.user", "sa");
        map.put("embedded.mssqlserver.password", properties.getPassword());

        String jdbcURL = "jdbc:sqlserver://{}:{};databaseName={};trustServerCertificate=true";
        log.info("Started mssql server. Connection details: {}, " +
                "JDBC connection url: " + jdbcURL, map, host, mappedPort, "master");

        MapPropertySource propertySource = new MapPropertySource("embeddedMSSQLServerInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

}
