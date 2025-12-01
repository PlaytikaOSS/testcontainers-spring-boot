package com.playtika.testcontainer.localstack;

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
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.localstack.LocalStackProperties.BEAN_NAME_EMBEDDED_LOCALSTACK;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(name = "com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration")
@ConditionalOnProperty(name = "embedded.localstack.enabled", matchIfMissing = true)
@EnableConfigurationProperties(LocalStackProperties.class)
public class EmbeddedLocalStackBootstrapConfiguration {

    private static final String LOCALSTACK_NETWORK_ALIAS = "localstack.testcontainer.docker";

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "localstack")
    ToxiproxyClientProxy localstackContainerProxy(ToxiproxyClient toxiproxyClient,
                                                   ToxiproxyContainer toxiproxyContainer,
                                                   @Qualifier(BEAN_NAME_EMBEDDED_LOCALSTACK) LocalStackContainer localStack,
                                                   LocalStackProperties properties) {
        return ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                localStack,
                properties.getEdgePort(),
                "localstack");
    }

    @ConditionalOnMissingBean(name = BEAN_NAME_EMBEDDED_LOCALSTACK)
    @Bean(name = BEAN_NAME_EMBEDDED_LOCALSTACK, destroyMethod = "stop")
    public LocalStackContainer localStack(ConfigurableEnvironment environment,
                                          LocalStackProperties properties,
                                          Optional<Network> network) {
        LocalStackContainer localStackContainer = new LocalStackContainer(ContainerUtils.getDockerImageName(properties));
        localStackContainer
                .withExposedPorts(properties.getEdgePort())
                .withEnv("EDGE_PORT", String.valueOf(properties.getEdgePort()))
                .withEnv("HOSTNAME", properties.getHostname())
                .withEnv("LOCALSTACK_HOST", properties.getHostnameExternal())
                .withEnv("SKIP_SSL_CERT_DOWNLOAD", "1")
                .withNetworkAliases(LOCALSTACK_NETWORK_ALIAS);

        network.ifPresent(localStackContainer::withNetwork);

        for (LocalStackContainer.Service service : properties.services) {
            localStackContainer.withServices(service);
        }
        localStackContainer = (LocalStackContainer) configureCommonsAndStart(localStackContainer, properties, log);
        return localStackContainer;
    }


    private static void setSystemProperties(LocalStackContainer localStack) {
        System.setProperty("aws.endpointUrl", localStack.getEndpoint().toString());
        System.setProperty("aws.accessKeyId", localStack.getAccessKey());
        System.setProperty("aws.secretAccessKey", localStack.getSecretKey());
    }

    @Bean
    public DynamicPropertyRegistrar localStackDynamicPropertyRegistrar(
            @Qualifier(BEAN_NAME_EMBEDDED_LOCALSTACK) LocalStackContainer localStack,
            LocalStackProperties properties) {
        return registry -> {
            String host = localStack.getHost();
            Integer mappedPort = localStack.getMappedPort(properties.getEdgePort());
            registry.add("embedded.localstack.host", () -> host);
            registry.add("embedded.localstack.port", () -> mappedPort);
            registry.add("embedded.localstack.endpointUrl", () -> localStack.getEndpoint().toString());
            registry.add("embedded.localstack.accessKey", localStack::getAccessKey);
            registry.add("embedded.localstack.secretAccessKey", localStack::getSecretKey);
            registry.add("embedded.localstack.networkAlias", () -> LOCALSTACK_NETWORK_ALIAS);
            registry.add("embedded.localstack.internalPort", properties::getEdgePort);
            registry.add("embedded.localstack.internalEdgePort", properties::getEdgePort);
            for (LocalStackContainer.Service service : properties.services) {
                registry.add("embedded.localstack." + service, () -> localStack.getEndpointOverride(service));
                registry.add("embedded.localstack." + service + ".port", () -> mappedPort);
            }
            setSystemProperties(localStack);
        };
    }

}
