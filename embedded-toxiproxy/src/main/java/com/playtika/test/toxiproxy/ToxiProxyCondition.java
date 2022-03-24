package com.playtika.test.toxiproxy;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.HashMap;
import java.util.Map;

class ToxiProxyCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        ConditionMessage.Builder message = ConditionMessage.forCondition(
                "Toxi Proxy Proxies Configured Condition");

        ToxiProxyProxiesProperties properties = getProperties(context);

        if (properties == null) {
            return ConditionOutcome.noMatch(message.notAvailable("toxi proxy properties"));
        }

        if (properties.getEnabled() != null && !properties.getEnabled()) {
            return ConditionOutcome.noMatch(message.because("toxi proxy is disabled"));
        }

        if (properties.getEnabled() != null && properties.getEnabled()) {
            return ConditionOutcome.match(message.because("toxi proxy is enabled"));
        }

        if (properties.getProxies().isEmpty()) {
            return ConditionOutcome.noMatch(message.didNotFind("proxies").atAll());
        }

        boolean atLeastOneProxyEnabled = properties.getProxies().entrySet().stream()
                .anyMatch(e -> e.getValue().isEnabled());

        if (atLeastOneProxyEnabled) {
            return ConditionOutcome.match(message.found("enabled").items("proxy"));
        }
        return ConditionOutcome.noMatch(message.didNotFind("enabled").items("proxy"));
    }

    private ToxiProxyProxiesProperties getProperties(ConditionContext context) {
        return Binder.get(context.getEnvironment())
                .bind("embedded.toxiproxy", ToxiProxyProxiesProperties.class)
                .orElse(null);
    }

    @ConfigurationProperties("embedded.toxiproxy")
    @Data
    public static class ToxiProxyProxiesProperties {

        Boolean enabled;
        Map<String, ModuleProxyProperties> proxies = new HashMap<>();


        @Data
        public static class ModuleProxyProperties {

            boolean enabled;
        }
    }
}
