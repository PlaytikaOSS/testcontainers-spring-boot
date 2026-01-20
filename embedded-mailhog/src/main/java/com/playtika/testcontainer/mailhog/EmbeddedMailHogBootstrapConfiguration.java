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
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
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
@AutoConfigureBefore(MailSenderAutoConfiguration.class)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
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

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.mailhog.smtp.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.mailhog.smtp.toxiproxy.port", proxy.getProxyPort());
        map.put("embedded.mailhog.smtp.toxiproxy.proxyName", proxy.getName());

        log.info("Started MailHog ToxiProxy connection details {}", Map.of(
                "embedded.mailhog.smtp.toxiproxy.host", proxy.getContainerIpAddress(),
                "embedded.mailhog.smtp.toxiproxy.port", proxy.getProxyPort(),
                "embedded.mailhog.smtp.toxiproxy.proxyName", proxy.getName()
        ));

        MapPropertySource propertySource = new MapPropertySource("embeddedMailhogToxiProxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);

        return proxy;
    }

    @ConditionalOnMissingBean(name = BEAN_NAME_EMBEDDED_MAILHOG)
    @Bean(name = BEAN_NAME_EMBEDDED_MAILHOG, destroyMethod = "stop")
    public GenericContainer<?> mailHog(MailHogProperties properties, ConfigurableEnvironment environment, Optional<Network> network) {
        GenericContainer<?> mailHog = new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                .withExposedPorts(properties.getSmtpPort(), properties.getHttpPort())
                .withNetworkAliases(MAILHOG_NETWORK_ALIAS)
                .waitingFor(Wait.forListeningPort());

        network.ifPresent(mailHog::withNetwork);

        mailHog = configureCommonsAndStart(mailHog, properties, log);
        registerMailhogEnvironment(mailHog, environment, properties);
        return mailHog;
    }

    private void registerMailhogEnvironment(GenericContainer<?> mailHog, ConfigurableEnvironment environment, MailHogProperties properties) {
        Integer smtpMappedPort = mailHog.getMappedPort(properties.getSmtpPort());
        Integer httpMappedPort = mailHog.getMappedPort(properties.getHttpPort());
        String host = mailHog.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.mailhog.host", host);
        map.put("embedded.mailhog.smtp-port", smtpMappedPort);
        map.put("embedded.mailhog.http-port", httpMappedPort);
        map.put("embedded.mailhog.networkAlias", MAILHOG_NETWORK_ALIAS);
        map.put("embedded.mailhog.internalSmtpPort", properties.getSmtpPort());
        map.put("embedded.mailhog.internalHttpPort", properties.getHttpPort());

        log.info("Started MailHog. Connection details: {}", Map.of(
                "embedded.mailhog.host", host,
                "embedded.mailhog.smtp-port", smtpMappedPort,
                "embedded.mailhog.http-port", httpMappedPort,
                "embedded.mailhog.networkAlias", MAILHOG_NETWORK_ALIAS,
                "embedded.mailhog.internalSmtpPort", properties.getSmtpPort(),
                "embedded.mailhog.internalHttpPort", properties.getHttpPort()
        ));

        MapPropertySource propertySource = new MapPropertySource("embeddedMailhogInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
