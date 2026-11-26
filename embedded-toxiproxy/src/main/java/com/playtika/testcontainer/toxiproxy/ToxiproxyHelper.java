package com.playtika.testcontainer.toxiproxy;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerNetwork;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Helper class for creating and managing Toxiproxy proxies using ToxiproxyClient
 */
@Slf4j
@UtilityClass
public class ToxiproxyHelper {

    private static final int FIRST_PROXIED_PORT = 8666;
    private static final int LAST_PROXIED_PORT = 8866;
    private static final AtomicInteger nextPort = new AtomicInteger(FIRST_PROXIED_PORT);
    private static final Pattern GENERATED_ALIAS_PATTERN = Pattern.compile("^tc-[1-9A-HJ-NP-Za-km-z]{8}$");

    /**
     * Creates a proxy for the given container using ToxiproxyClient.
     * This method replicates the behavior of ToxiproxyContainer.getProxy() including
     * sequential port allocation and port mapping.
     *
     * @param toxiproxyClient    The ToxiproxyClient instance
     * @param toxiproxyContainer The ToxiproxyContainer instance (for port mapping)
     * @param targetContainer    The target container to proxy
     * @param targetPort         The target port on the container
     * @param proxyName          The name for the proxy
     * @return ToxiproxyClientProxy wrapping the created proxy
     */
    public static ToxiproxyClientProxy createProxy(ToxiproxyClient toxiproxyClient,
                                                    ToxiproxyContainer toxiproxyContainer,
                                                    GenericContainer<?> targetContainer,
                                                    int targetPort,
                                                    String proxyName) {
        try {
            // Get the target container's network alias
            String upstream = getUpstreamAddress(targetContainer, targetPort);

            // Allocate a sequential port (matching original ToxiproxyContainer behavior)
            final int toxiPort = nextPort.getAndIncrement();
            if (toxiPort > LAST_PROXIED_PORT) {
                throw new IllegalStateException("Maximum number of proxies exceeded");
            }

            // Create the proxy - listen on the specific port and forward to upstream
            Proxy proxy = toxiproxyClient.createProxy(proxyName, "0.0.0.0:" + toxiPort, upstream);

            // Map the internal toxiproxy port to the external host port
            final int mappedPort = toxiproxyContainer.getMappedPort(toxiPort);

            return new ToxiproxyClientProxy(proxy, toxiproxyContainer.getHost(), mappedPort);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create toxiproxy for " + proxyName, e);
        }
    }

    /**
     * Creates a proxy for a target hostname using ToxiproxyClient.
     * This is useful when the target is identified by hostname rather than a container instance,
     * such as for Kafka which uses network aliases.
     *
     * @param toxiproxyClient    The ToxiproxyClient instance
     * @param toxiproxyContainer The ToxiproxyContainer instance (for port mapping)
     * @param targetHostname     The target hostname to proxy
     * @param targetPort         The target port
     * @param proxyName          The name for the proxy
     * @return ToxiproxyClientProxy wrapping the created proxy
     */
    public static ToxiproxyClientProxy createProxy(ToxiproxyClient toxiproxyClient,
                                                    ToxiproxyContainer toxiproxyContainer,
                                                    String targetHostname,
                                                    int targetPort,
                                                    String proxyName) {
        try {
            String upstream = targetHostname + ":" + targetPort;

            // Allocate a sequential port (matching original ToxiproxyContainer behavior)
            final int toxiPort = nextPort.getAndIncrement();
            if (toxiPort > LAST_PROXIED_PORT) {
                throw new IllegalStateException("Maximum number of proxies exceeded");
            }

            // Create the proxy - listen on the specific port and forward to upstream
            Proxy proxy = toxiproxyClient.createProxy(proxyName, "0.0.0.0:" + toxiPort, upstream);

            // Map the internal toxiproxy port to the external host port
            final int mappedPort = toxiproxyContainer.getMappedPort(toxiPort);

            return new ToxiproxyClientProxy(proxy, toxiproxyContainer.getHost(), mappedPort);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create toxiproxy for " + proxyName, e);
        }
    }

    /**
     * Gets the upstream address for a container.
     * Prefers an explicit (non-generated) network alias so container reuse works reliably
     * (Testcontainers prepends a new random {@code tc-<8chars>} alias at index 0 on every JVM run,
     * which does not exist on a reused container's Docker network).
     * If no explicit alias is configured, inspects the running container's actual Docker network
     * aliases/IP before falling back to any configured alias.
     */
    static String getUpstreamAddress(GenericContainer<?> container, int port) {
        List<String> networkAliases = container.getNetworkAliases();
        if (networkAliases != null && !networkAliases.isEmpty()) {
            String explicitAlias = networkAliases.stream()
                    .filter(alias -> alias != null && !alias.isBlank() && !isGeneratedAlias(alias))
                    .findFirst()
                    .orElse(null);
            if (explicitAlias != null) {
                return explicitAlias + ":" + port;
            }
        }

        String liveAddress = getLiveContainerAddress(container);
        if (liveAddress != null) {
            return liveAddress + ":" + port;
        }

        if (networkAliases != null) {
            String fallbackAlias = networkAliases.stream()
                    .filter(alias -> alias != null && !alias.isBlank())
                    .findFirst()
                    .orElse(null);
            if (fallbackAlias != null) {
                return fallbackAlias + ":" + port;
            }
        }

        throw new IllegalStateException("Cannot determine upstream address for container");
    }

    private static boolean isGeneratedAlias(String alias) {
        return GENERATED_ALIAS_PATTERN.matcher(alias).matches();
    }

    private static String getLiveContainerAddress(GenericContainer<?> container) {
        InspectContainerResponse containerInfo = container.getContainerInfo();
        if (containerInfo == null || containerInfo.getNetworkSettings() == null) {
            return null;
        }
        Map<String, ContainerNetwork> networks = containerInfo.getNetworkSettings().getNetworks();
        if (networks == null || networks.isEmpty()) {
            return null;
        }
        for (ContainerNetwork network : networks.values()) {
            if (network != null && network.getAliases() != null) {
                for (String alias : network.getAliases()) {
                    if (alias != null && !alias.isBlank()) {
                        return alias;
                    }
                }
            }
        }
        for (ContainerNetwork network : networks.values()) {
            if (network != null && network.getIpAddress() != null && !network.getIpAddress().isBlank()) {
                return network.getIpAddress();
            }
        }
        return null;
    }

    /**
     * Registers environment properties for a toxiproxy connection
     */
    public static void registerProxyEnvironment(ToxiproxyClientProxy proxy,
                                                 String propertyPrefix,
                                                 String propertySourceName,
                                                 ConfigurableEnvironment environment) {
        registerProxyEnvironment(proxy, propertyPrefix, propertySourceName, environment, "port");
    }

    /**
     * Registers environment properties for a toxiproxy connection with a custom port key.
     * This is useful for cases like Azurite which has multiple ports (blobStoragePort, queueStoragePort, etc.)
     */
    public static void registerProxyEnvironment(ToxiproxyClientProxy proxy,
                                                 String propertyPrefix,
                                                 String propertySourceName,
                                                 ConfigurableEnvironment environment,
                                                 String portKey) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(propertyPrefix + ".toxiproxy.host", proxy.getContainerIpAddress());
        map.put(propertyPrefix + ".toxiproxy." + portKey, proxy.getProxyPort());
        map.put(propertyPrefix + ".toxiproxy.proxyName", proxy.getName());

        MapPropertySource propertySource = new MapPropertySource(propertySourceName, map);
        environment.getPropertySources().addFirst(propertySource);
        log.info("Started {} ToxiProxy connection details {}", propertyPrefix, map);
    }
}
