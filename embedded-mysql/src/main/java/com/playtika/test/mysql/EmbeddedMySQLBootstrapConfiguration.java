package com.playtika.test.mysql;

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
import org.testcontainers.containers.MySQLContainer;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.test.mysql.MySQLProperties.BEAN_NAME_EMBEDDED_MYSQL;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.mysql.enabled", matchIfMissing = true)
@EnableConfigurationProperties(MySQLProperties.class)
public class EmbeddedMySQLBootstrapConfiguration {
    @Bean(name = BEAN_NAME_EMBEDDED_MYSQL, destroyMethod = "stop")
    public MySQLContainer mysql(ConfigurableEnvironment environment,
                                MySQLProperties properties) {
      MySQLContainer mysql =
                new MySQLContainer<>(ContainerUtils.getDockerImageName(properties))
                        .withEnv("MYSQL_ALLOW_EMPTY_PASSWORD", "yes")
                        .withUsername(properties.getUser())
                        .withDatabaseName(properties.getDatabase())
                        .withPassword(properties.getPassword())
                        .withCommand(buildCommands(properties))
                        .withExposedPorts(properties.port)
                        .withInitScript(properties.initScriptPath);
        mysql = (MySQLContainer) configureCommonsAndStart(mysql, properties, log);
        registerMySQLEnvironment(mysql, environment, properties);
        return mysql;
    }

  private String[] buildCommands(MySQLProperties properties) {
    List<String> commands = Arrays.asList(
      "--character-set-server=" + properties.getEncoding(),
      "--collation-server=" + properties.getCollation()
    );
    boolean mysqlVersion8x = properties.getDockerImageVersion().startsWith("8");
    if (mysqlVersion8x) {
      commands.add("--default-authentication-plugin=mysql_native_password");
    }
    return commands.toArray(new String[0]);
  }

  private void registerMySQLEnvironment(MySQLContainer mysql,
                                          ConfigurableEnvironment environment,
                                          MySQLProperties properties) {
        Integer mappedPort = mysql.getMappedPort(properties.port);
        String host = mysql.getContainerIpAddress();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.mysql.port", mappedPort);
        map.put("embedded.mysql.host", host);
        map.put("embedded.mysql.schema", properties.getDatabase());
        map.put("embedded.mysql.user", properties.getUser());
        map.put("embedded.mysql.password", properties.getPassword());

        String jdbcURL = "jdbc:mysql://{}:{}/{}";
        log.info("Started mysql server. Connection details: {}, " +
                "JDBC connection url: " + jdbcURL, map, host, mappedPort, properties.getDatabase());

        MapPropertySource propertySource = new MapPropertySource("embeddedMySQLInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
