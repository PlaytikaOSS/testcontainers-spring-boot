package com.playtika.test.toxiproxy.condition;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class ToxiProxyConditionSpecificModuleTest {

    static final String BEAN = "beanEnabledByToxiProxyCondition";
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void disabledByDefault() {
        this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(BEAN));
    }

    @Test
    void enabledIfGlobalPropertyIsEnabled() {
        this.contextRunner.withPropertyValues("embedded.toxiproxy.enabled=true")
                .run((context) -> assertThat(context).hasBean("beanEnabledByToxiProxyCondition"));
    }

    @Test
    void disabledIfGlobalPropertyIsDisabled() {
        this.contextRunner.withPropertyValues("embedded.toxiproxy.enabled=false")
                .run((context) -> assertThat(context).doesNotHaveBean("beanEnabledByToxiProxyCondition"));
    }

    @Test
    void enabledIfProxyPropertyIsEnabled() {
        this.contextRunner.withPropertyValues("embedded.toxiproxy.proxies.aerospike.enabled=true")
                .run((context) -> assertThat(context).hasBean("beanEnabledByToxiProxyCondition"));
    }

    @Test
    void disabledIfOtherProxyPropertyIsEnabled() {
        this.contextRunner.withPropertyValues("embedded.toxiproxy.proxies.notaerospike.enabled=true")
                .run((context) -> assertThat(context).doesNotHaveBean("beanEnabledByToxiProxyCondition"));
    }

    @Test
    void disabledIfProxyPropertyIsDisabled() {
        this.contextRunner.withPropertyValues("embedded.toxiproxy.proxies.aerospike.enabled=false")
                .run((context) -> assertThat(context).doesNotHaveBean("beanEnabledByToxiProxyCondition"));
    }

    @Test
    void globalPropertyDisablesEvenWhenProxyPropertyIsEnabled() {
        this.contextRunner.withPropertyValues(
                        "embedded.toxiproxy.enabled=false",
                        "embedded.toxiproxy.proxies.aerospike.enabled=true"
                )
                .run((context) -> assertThat(context).doesNotHaveBean("beanEnabledByToxiProxyCondition"));
    }

    @Configuration(proxyBeanMethods = false)
    static class TestConfig {

        @Bean(name = BEAN)
        @ConditionalOnToxiProxyEnabled(module = "aerospike")
        String beanEnabledByToxiProxyCondition() {
            return "beanEnabledByToxiProxyCondition";
        }

    }

}