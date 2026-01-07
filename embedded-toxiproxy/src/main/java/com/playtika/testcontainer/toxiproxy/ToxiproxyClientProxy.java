package com.playtika.testcontainer.toxiproxy;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.model.ToxicList;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

/**
 * Wrapper class that bridges between toxiproxy-java's Proxy and the existing API
 * that was previously provided by ToxiproxyClientProxy.
 * This maintains backward compatibility while using the standalone ToxiproxyClient.
 */
@RequiredArgsConstructor
public class ToxiproxyClientProxy {

    private final Proxy proxy;
    @Getter
    private final String containerIpAddress;
    private final int mappedPort;

    // Track enabled/disabled state since toxiproxy-java Proxy doesn't expose this
    private boolean connectionCut = false;

    /**
     * Returns the proxy port accessible from the host machine (the mapped port)
     */
    public int getProxyPort() {
        return mappedPort;
    }

    /**
     * Returns the internal toxiproxy port (inside the container)
     */
    public int getOriginalProxyPort() {
        // The listen address is in format "host:port", extract the port
        String listen = proxy.getListen();
        return Integer.parseInt(listen.substring(listen.lastIndexOf(':') + 1));
    }

    /**
     * Returns the proxy name
     */
    public String getName() {
        return proxy.getName();
    }

    /**
     * Returns the toxics list for managing network conditions
     */
    public ToxicList toxics() {
        return proxy.toxics();
    }

    /**
     * Returns the underlying toxiproxy-java Proxy object
     */
    public Proxy getProxy() {
        return proxy;
    }

    /**
     * Enable or disable the proxy connection
     * @param connectionCut true to cut the connection, false to restore it
     */
    @SneakyThrows
    public void setConnectionCut(boolean connectionCut) {
        this.connectionCut = connectionCut;
        if (connectionCut) {
            proxy.disable();
        } else {
            proxy.enable();
        }
    }

    /**
     * Check if the connection is cut (proxy is disabled)
     * @return true if connection is cut
     */
    public boolean isConnectionCut() {
        return connectionCut;
    }

}
