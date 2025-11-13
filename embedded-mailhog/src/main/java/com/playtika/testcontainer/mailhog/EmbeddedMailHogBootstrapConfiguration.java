package com.playtika.testcontainer.mailhog;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.toxiproxy.ToxiproxyClientProxy;
import com.playtika.testcontainer.toxiproxy.ToxiproxyHelper;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import eu.rekawek.toxiproxy.ToxiproxyClient;
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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.mailhog.MailHogProperties.BEAN_NAME_EMBEDDED_MAILHOG;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.mailhog.enabled", matchIfMissing = true)
@EnableConfigurationProperties(MailHogProperties.class)
public class EmbeddedMailHogBootstrapConfiguration {

    private static final String MAILHOG_NETWORK_ALIAS = "mailhog.testcontainer.docker";

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "mailhog")
    ToxiproxyClientProxy mailhogSmtpContainerProxy(ToxiproxyClient toxiproxyClient,
                                                    ToxiproxyContainer toxiproxyContainer,
                                                    @Qualifier(BEAN_NAME_EMBEDDED_MAILHOG) GenericContainer<?> mailhogContainer,
                                                    MailHogProperties properties,
                                                    ConfigurableEnvironment environment) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                mailhogContainer,
                properties.getSmtpPort(),
                "mailhog");

        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.mailhog.smtp", "embeddedMailhogSmtpToxiproxyInfo", environment);

        return proxy;
    }

    @ConditionalOnMissingBean(name = BEAN_NAME_EMBEDDED_MAILHOG)
    @Bean(name = BEAN_NAME_EMBEDDED_MAILHOG, destroyMethod = "stop")
    public GenericContainer<?> mailHog(ConfigurableEnvironment environment,
                                       MailHogProperties properties,
                                       Optional<Network> network) {
        GenericContainer<?> mailHog = new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                .withExposedPorts(properties.getSmtpPort(), properties.getHttpPort())
                .withNetworkAliases(MAILHOG_NETWORK_ALIAS)
                .waitingFor(Wait.forListeningPort());

        network.ifPresent(mailHog::withNetwork);

        mailHog = configureCommonsAndStart(mailHog, properties, log);
        registerMailHogEnvironment(mailHog, environment, properties);
        return mailHog;
    }

    private void registerMailHogEnvironment(GenericContainer<?> mailHog, ConfigurableEnvironment environment, MailHogProperties properties) {
        Integer smtpMappedPort = mailHog.getMappedPort(properties.getSmtpPort());
        Integer httpMappedPort = mailHog.getMappedPort(properties.getHttpPort());

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.mailhog.host", mailHog.getHost());
        map.put("embedded.mailhog.smtp-port", smtpMappedPort);
        map.put("embedded.mailhog.http-port", httpMappedPort);
        map.put("embedded.mailhog.networkAlias", MAILHOG_NETWORK_ALIAS);
        map.put("embedded.mailhog.internalSmtpPort", properties.getSmtpPort());
        map.put("embedded.mailhog.internalHttpPort", properties.getHttpPort());

        log.info("Started MailHog. Connection details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedMailHogInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

}
