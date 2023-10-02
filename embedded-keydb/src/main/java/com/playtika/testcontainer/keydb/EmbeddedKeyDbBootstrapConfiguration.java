package com.playtika.testcontainer.keydb;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.common.utils.FileUtils;
import com.playtika.testcontainer.keydb.wait.DefaultKeyDbClusterWaitStrategy;
import com.playtika.testcontainer.keydb.wait.KeyDbStatusCheck;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import lombok.RequiredArgsConstructor;
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
import org.springframework.core.io.ResourceLoader;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.keydb.EnvUtils.registerKeyDbEnvironment;
import static com.playtika.testcontainer.keydb.KeyDbProperties.BEAN_NAME_EMBEDDED_KEYDB;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.keydb.enabled", matchIfMissing = true)
@EnableConfigurationProperties(KeyDbProperties.class)
@RequiredArgsConstructor
public class EmbeddedKeyDbBootstrapConfiguration {

  public static final String KEYDB_NETWORK_ALIAS = "keydb.testcontainer.docker";
  public static final String KEYDB_WAIT_STRATEGY_BEAN_NAME = "keydbStartupCheckStrategy";

  private final ResourceLoader resourceLoader;
  private final KeyDbProperties properties;

  @Bean(name = KEYDB_WAIT_STRATEGY_BEAN_NAME)
  @ConditionalOnMissingBean(name = KEYDB_WAIT_STRATEGY_BEAN_NAME)
  @ConditionalOnProperty(name = "embedded.keydb.clustered", havingValue = "false", matchIfMissing = true)
  public WaitStrategy keydbStartupCheckStrategy(KeyDbProperties properties) {
    return new KeyDbStatusCheck(properties);
  }

  @Bean(name = KEYDB_WAIT_STRATEGY_BEAN_NAME)
  @ConditionalOnMissingBean(name = KEYDB_WAIT_STRATEGY_BEAN_NAME)
  @ConditionalOnProperty(name = "embedded.keydb.clustered", havingValue = "true")
  public WaitStrategy keydbClusterWaitStrategy(KeyDbProperties properties) {
    return new DefaultKeyDbClusterWaitStrategy(properties);
  }

  @Bean
  @ConditionalOnToxiProxyEnabled(module = "keydb")
  ToxiproxyContainer.ContainerProxy keydbContainerProxy(ToxiproxyContainer toxiproxyContainer,
                                                        @Qualifier(BEAN_NAME_EMBEDDED_KEYDB) GenericContainer<?> keydb,
                                                        KeyDbProperties properties,
                                                        ConfigurableEnvironment environment) {
    ToxiproxyContainer.ContainerProxy proxy = toxiproxyContainer.getProxy(keydb, properties.getPort());

    Map<String, Object> map = new LinkedHashMap<>();
    map.put("embedded.keydb.toxiproxy.host", proxy.getContainerIpAddress());
    map.put("embedded.keydb.toxiproxy.port", proxy.getProxyPort());
    map.put("embedded.keydb.toxiproxy.proxyName", proxy.getName());

    MapPropertySource propertySource = new MapPropertySource("embeddedKeyDbToxiProxyInfo", map);
    environment.getPropertySources().addFirst(propertySource);
    log.info("Started KeyDb ToxiProxy connection details {}", map);

    return proxy;
  }

  @Bean(name = BEAN_NAME_EMBEDDED_KEYDB, destroyMethod = "stop")
  public GenericContainer<?> keydb(ConfigurableEnvironment environment,
                                   @Qualifier(KEYDB_WAIT_STRATEGY_BEAN_NAME) WaitStrategy keydbStartupCheckStrategy,
                                   Optional<Network> network) throws Exception {

    // CLUSTER SLOTS command returns IP:port for each node, so ports outside and inside
    // container must be the same
    GenericContainer<?> keydb =
      new FixedHostPortGenericContainer(ContainerUtils.getDockerImageName(properties).asCanonicalNameString())
        .withFixedExposedPort(properties.getPort(), properties.getPort())
        .withExposedPorts(properties.getPort())
        .withEnv("KEYDB_USER", properties.getUser())
        .withEnv("KEYDB_PASSWORD", properties.getPassword())
        .withCopyFileToContainer(MountableFile.forHostPath(prepareKeyDbConf()), "/data/keydb.conf")
        .withCopyFileToContainer(MountableFile.forHostPath(prepareNodesConf()), "/data/nodes.conf")
        .withCommand("keydb-server", "/data/keydb.conf")
        .waitingFor(keydbStartupCheckStrategy)
        .withNetworkAliases(KEYDB_NETWORK_ALIAS);
    network.ifPresent(keydb::withNetwork);
    keydb = configureCommonsAndStart(keydb, properties, log);
    Map<String, Object> keydbEnv = registerKeyDbEnvironment(environment, keydb, properties, properties.getPort());
    log.info("Started KeyDb cluster. Connection details: {}", keydbEnv);
    return keydb;
  }

  private Path prepareKeyDbConf() throws IOException {
    return FileUtils.resolveTemplateAsPath(resourceLoader, "keydb.conf", content -> content
      .replace("{{requirepass}}", properties.isRequirepass() ? "yes" : "no")
      .replace("{{password}}", properties.isRequirepass() ? "requirepass " + properties.getPassword() : "")
      .replace("{{clustered}}", properties.isClustered() ? "yes" : "no")
      .replace("{{port}}", String.valueOf(properties.getPort())));
  }

  private Path prepareNodesConf() throws IOException {
    return FileUtils.resolveTemplateAsPath(resourceLoader, "nodes.conf", content -> content
      .replace("{{port}}", String.valueOf(properties.getPort()))
      .replace("{{busPort}}", String.valueOf(properties.getPort() + 10000)));
  }

}
