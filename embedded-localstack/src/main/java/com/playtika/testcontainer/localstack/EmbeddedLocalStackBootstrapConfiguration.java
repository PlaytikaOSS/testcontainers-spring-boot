package com.playtika.testcontainer.localstack;

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
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;

import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.localstack.LocalStackProperties.BEAN_NAME_EMBEDDED_LOCALSTACK;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.localstack.enabled", matchIfMissing = true)
@EnableConfigurationProperties(LocalStackProperties.class)
public class EmbeddedLocalStackBootstrapConfiguration {

    private static final String LOCALSTACK_NETWORK_ALIAS = "localstack.testcontainer.docker";

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "localstack")
    ToxiproxyClientProxy localstackContainerProxy(ToxiproxyClient toxiproxyClient,
                                                   ToxiproxyContainer toxiproxyContainer,
                                                   @Qualifier(BEAN_NAME_EMBEDDED_LOCALSTACK) LocalStackContainer localStack,
                                                   LocalStackProperties properties,
                                                   ConfigurableEnvironment environment) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                localStack,
                properties.getEdgePort(),
                "localstack");

        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.localstack", "embeddedLocalstackToxiproxyInfo", environment);

        return proxy;
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "localstack")
    public DynamicPropertyRegistrar localstackToxiProxyDynamicPropertyRegistrar(
        @Qualifier("localstackContainerProxy") ToxiproxyContainer.ContainerProxy proxy) {
        return registry -> {
            registry.add("embedded.localstack.toxiproxy.host", proxy::getContainerIpAddress);
            registry.add("embedded.localstack.toxiproxy.port", proxy::getProxyPort);
            registry.add("embedded.localstack.toxiproxy.proxyName", proxy::getName);
        };
    }

    @ConditionalOnMissingBean(name = BEAN_NAME_EMBEDDED_LOCALSTACK)
    @Bean(name = BEAN_NAME_EMBEDDED_LOCALSTACK, destroyMethod = "stop")
    public LocalStackContainer localStack(LocalStackProperties properties, Optional<Network> network) {
        LocalStackContainer localStack = new LocalStackContainer(ContainerUtils.getDockerImageName(properties))
                .withExposedPorts(properties.getEdgePort())
                .withNetworkAliases(LOCALSTACK_NETWORK_ALIAS);
        network.ifPresent(localStack::withNetwork);
        configureCommonsAndStart(localStack, properties, log);
        return localStack;
    }

    private void registerLocalStackEnvironment(LocalStackContainer localStack,
                                               ConfigurableEnvironment environment,
                                               LocalStackProperties properties) {
        String host = localStack.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.localstack.host", host);
        map.put("embedded.localstack.endpointUrl", localStack.getEndpoint().toString());
        map.put("embedded.localstack.accessKey", localStack.getAccessKey());
        map.put("embedded.localstack.secretAccessKey", localStack.getSecretKey());
        map.put("embedded.localstack.networkAlias", LOCALSTACK_NETWORK_ALIAS);
        map.put("embedded.localstack.internalEdgePort", properties.getEdgePort());
        String prefix = "embedded.localstack.";
        Integer mappedPort = localStack.getMappedPort(properties.getEdgePort());
        for (LocalStackContainer.Service service : properties.services) {
            map.put(prefix + service, localStack.getEndpointOverride(service));
            map.put(prefix + service + ".port", mappedPort);
        }
        log.info("Started Localstack. Connection details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedLocalStackInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        setSystemProperties(localStack);
    }

    private static void setSystemProperties(LocalStackContainer localStack) {
        System.setProperty("aws.endpointUrl", localStack.getEndpoint().toString());
        System.setProperty("aws.accessKeyId", localStack.getAccessKey());
        System.setProperty("aws.secretAccessKey", localStack.getSecretKey());
    @Bean
    public DynamicPropertyRegistrar localStackDynamicPropertyRegistrar(
            @Qualifier(BEAN_NAME_EMBEDDED_LOCALSTACK) LocalStackContainer localStack,
            LocalStackProperties properties) {
        return registry -> {
            registry.add("embedded.localstack.host", localStack::getHost);
            registry.add("embedded.localstack.port", () -> localStack.getMappedPort(properties.getEdgePort()));
            registry.add("embedded.localstack.networkAlias", () -> LOCALSTACK_NETWORK_ALIAS);
            registry.add("embedded.localstack.internalPort", properties::getEdgePort);
        };
    }

}
