package com.playtika.testcontainer.toxiproxy.condition;

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
import java.util.Set;
import java.util.stream.Collectors;

public class ToxiProxyCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnToxiProxyEnabled.class.getName());
        String module = (String) attributes.get("module");

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

        Set<String> enabledProxies = properties.getProxies().entrySet().stream()
                .filter(e -> e.getValue().isEnabled())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        if (module.isEmpty()) {
            if (!enabledProxies.isEmpty()) {
                return ConditionOutcome.match(message.found("enabled").items(enabledProxies));
            }
        } else {
            if (enabledProxies.contains(module)) {
                return ConditionOutcome.match(message.found("enabled").items(module));
            }
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
