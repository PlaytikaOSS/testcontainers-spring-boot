package com.playtika.test.mongodb;

import com.github.dockerjava.api.model.Capability;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

import static java.lang.String.format;

import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.containerLogsConsumer;
import static com.playtika.test.mongodb.MongodbProperties.BEAN_NAME_EMBEDDED_MONGODB;

@Slf4j
@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(
        name = "embedded.mongodb.enabled",
        havingValue = "true",
        matchIfMissing = true)
@EnableConfigurationProperties(MongodbProperties.class)
public class EmbeddedMongodbBootstrapConfiguration {

	private final ResourceLoader resourceLoader;

	@Autowired
	public EmbeddedMongodbBootstrapConfiguration(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

    @Bean(value = BEAN_NAME_EMBEDDED_MONGODB, destroyMethod = "stop")
    public GenericContainer mongodb(
            ConfigurableEnvironment environment,
            MongodbProperties properties,
            MongodbStatusCheck mongodbStatusCheck) {
        GenericContainer mongodb =
                new GenericContainer<>(properties.getDockerImage())
                        .withEnv("MONGO_INITDB_ROOT_USERNAME", properties.getUsername())
                        .withEnv("MONGO_INITDB_ROOT_PASSWORD", properties.getPassword())
                        .withEnv("MONGO_INITDB_DATABASE", properties.getDatabase())
                        .withLogConsumer(containerLogsConsumer(log))
                        .withExposedPorts(properties.getPort())
                        .withCreateContainerCmdModifier(cmd -> cmd.withCapAdd(Capability.NET_ADMIN))
                        .waitingFor(mongodbStatusCheck)
                        .withStartupTimeout(properties.getTimeoutDuration());
        withConfigFile(mongodb, properties.getConfigFile());
        
        mongodb.start();
        registerMongodbEnvironment(mongodb, environment, properties);
        return mongodb;
    }

    @Bean
    @ConditionalOnMissingBean
    MongodbStatusCheck mongodbStartupCheckStrategy(MongodbProperties properties) {
        return new MongodbStatusCheck();
    }

    private void registerMongodbEnvironment(GenericContainer mongodb, ConfigurableEnvironment environment, MongodbProperties properties) {
        Integer mappedPort = mongodb.getMappedPort(properties.getPort());
        String host = mongodb.getContainerIpAddress();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.mongodb.port", mappedPort);
        map.put("embedded.mongodb.host", host);
        map.compute("embedded.mongodb.username", (k, v) -> properties.getUsername());
        map.compute("embedded.mongodb.password", (k, v) -> properties.getPassword());
        map.put("embedded.mongodb.database", properties.getDatabase());

        log.info("Started mongodb. Connection Details: {}, Connection URI: mongodb://{}:{}/{}", map, host, mappedPort, properties.getDatabase());

        MapPropertySource propertySource = new MapPropertySource("embeddedMongoInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

    private void withConfigFile(GenericContainer container, String importFile) {
        if (importFile == null) {
            return;
        }

        checkExists(importFile);

        String importFileInContainer = "/tmp/" + importFile;
        container.withCopyFileToContainer(
        			MountableFile.forClasspathResource(importFile),
        			importFileInContainer
        )
        .withCommand("--config", importFileInContainer);
    }

    private void checkExists(String importFile) {
        Resource resource = resourceLoader.getResource("classpath:" + importFile);
        if (resource.exists()) {
            log.debug("Using config file: {}", resource.getFilename());
            return;
        }

        throw new ConfigFileNotFoundException(importFile);
    }

    public static final class ConfigFileNotFoundException extends IllegalArgumentException {

        private static final long serialVersionUID = 6350884245669857560L;

        ConfigFileNotFoundException(String configFile) {
            super(format(
                "Classpath resource '%s' defined through 'embedded.mongodb.config-file' does not exist.",
                configFile));
        }
    }
}
