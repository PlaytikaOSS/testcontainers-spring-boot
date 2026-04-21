package com.playtika.testcontainer.toxiproxy;

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
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper class for creating and managing Toxiproxy proxies using ToxiproxyClient
 */
@Slf4j
@UtilityClass
public class ToxiproxyHelper {

    private static final int FIRST_PROXIED_PORT = 8666;
    private static final int LAST_PROXIED_PORT = 8866;
    private static final AtomicInteger nextPort = new AtomicInteger(FIRST_PROXIED_PORT);

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
     * Uses the container's network aliases if available, otherwise uses the container's network address.
     */
    private static String getUpstreamAddress(GenericContainer<?> container, int port) {
        // Try to get network aliases first
        var networkAliases = container.getNetworkAliases();
        if (!networkAliases.isEmpty()) {
            // Use the first network alias
            String alias = networkAliases.get(0);
            if (alias != null && !alias.isEmpty()) {
                return alias + ":" + port;
            }
        }

        // Fallback to container network IP
        return container.getContainerInfo().getNetworkSettings().getNetworks()
                .values().stream()
                .findFirst()
                .map(network -> network.getIpAddress() + ":" + port)
                .orElseThrow(() -> new IllegalStateException("Cannot determine upstream address for container"));
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
