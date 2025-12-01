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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

import java.util.Map;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.mailhog.MailHogProperties.BEAN_NAME_EMBEDDED_MAILHOG;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@AutoConfigureBefore(name = "org.springframework.boot.mail.autoconfigure.MailSenderAutoConfiguration")
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
                                                    MailHogProperties properties) {
        return ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                mailhogContainer,
                properties.getSmtpPort(),
                "mailhog");
    }

    @ConditionalOnMissingBean(name = BEAN_NAME_EMBEDDED_MAILHOG)
    @Bean(name = BEAN_NAME_EMBEDDED_MAILHOG, destroyMethod = "stop")
    public GenericContainer<?> mailHog(MailHogProperties properties, Optional<Network> network) {
        GenericContainer<?> mailHog = new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                .withExposedPorts(properties.getSmtpPort(), properties.getHttpPort())
                .withNetworkAliases(MAILHOG_NETWORK_ALIAS)
                .waitingFor(Wait.forListeningPort());

        network.ifPresent(mailHog::withNetwork);

        return configureCommonsAndStart(mailHog, properties, log);
    }

    @Bean
    public DynamicPropertyRegistrar mailHogDynamicPropertyRegistrar(
            @Qualifier(BEAN_NAME_EMBEDDED_MAILHOG) GenericContainer<?> mailHog, MailHogProperties properties) {
        return registry -> {
            Integer smtpMappedPort = mailHog.getMappedPort(properties.getSmtpPort());
            Integer httpMappedPort = mailHog.getMappedPort(properties.getHttpPort());
            String host = mailHog.getHost();

            registry.add("embedded.mailhog.host", () -> host);
            registry.add("embedded.mailhog.smtp-port", () -> smtpMappedPort);
            registry.add("embedded.mailhog.http-port", () -> httpMappedPort);
            registry.add("embedded.mailhog.networkAlias", () -> MAILHOG_NETWORK_ALIAS);
            registry.add("embedded.mailhog.internalSmtpPort", () -> properties.getSmtpPort());
            registry.add("embedded.mailhog.internalHttpPort", () -> properties.getHttpPort());

            log.info("Started MailHog. Connection details: {}", Map.of(
                    "embedded.mailhog.host", host,
                    "embedded.mailhog.smtp-port", smtpMappedPort,
                    "embedded.mailhog.http-port", httpMappedPort,
                    "embedded.mailhog.networkAlias", MAILHOG_NETWORK_ALIAS,
                    "embedded.mailhog.internalSmtpPort", properties.getSmtpPort(),
                    "embedded.mailhog.internalHttpPort", properties.getHttpPort()
            ));
        };
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "mailhog")
    public DynamicPropertyRegistrar mailHogToxiProxyDynamicPropertyRegistrar(
            @Qualifier("mailhogSmtpContainerProxy") ToxiproxyClientProxy proxy) {
        return registry -> {
            registry.add("embedded.mailhog.smtp.toxiproxy.host", proxy::getContainerIpAddress);
            registry.add("embedded.mailhog.smtp.toxiproxy.port", proxy::getProxyPort);
            registry.add("embedded.mailhog.smtp.toxiproxy.proxyName", proxy::getName);

            log.info("Started MailHog ToxiProxy connection details {}", Map.of(
                    "embedded.mailhog.smtp.toxiproxy.host", proxy.getContainerIpAddress(),
                    "embedded.mailhog.smtp.toxiproxy.port", proxy.getProxyPort(),
                    "embedded.mailhog.smtp.toxiproxy.proxyName", proxy.getName()
            ));
        };
    }

}
