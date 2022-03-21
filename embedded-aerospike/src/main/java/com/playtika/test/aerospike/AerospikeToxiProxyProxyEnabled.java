package com.playtika.test.aerospike;

import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

class AerospikeToxiProxyProxyEnabled extends AllNestedConditions {

    AerospikeToxiProxyProxyEnabled() {
        super(ConfigurationPhase.PARSE_CONFIGURATION);
    }

    @ConditionalOnProperty(value = "embedded.toxiproxy.enabled", havingValue = "true", matchIfMissing = true)
    static class GlobalToxiProxyEnabled {
    }

    @ConditionalOnProperty(value = "embedded.toxiproxy.proxies.aerospike.enabled", havingValue = "true", matchIfMissing = false)
    static class AerospikeToxiProxyEnabled {
    }

}
