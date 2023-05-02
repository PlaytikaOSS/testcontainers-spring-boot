package com.playtika.testcontainer.toxiproxy.condition;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class ToxiProxyConditionAnyModuleTest {

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
        this.contextRunner.withPropertyValues("embedded.toxiproxy.proxies.any-module.enabled=true")
                .run((context) -> assertThat(context).hasBean("beanEnabledByToxiProxyCondition"));
    }

    @Test
    void disabledIfProxyPropertyIsDisabled() {
        this.contextRunner.withPropertyValues("embedded.toxiproxy.proxies.any-module.enabled=false")
                .run((context) -> assertThat(context).doesNotHaveBean("beanEnabledByToxiProxyCondition"));
    }

    @Test
    void globalPropertyDisablesEvenWhenProxyPropertyIsEnabled() {
        this.contextRunner.withPropertyValues(
                        "embedded.toxiproxy.enabled=false",
                        "embedded.toxiproxy.proxies.any-module.enabled=true"
                )
                .run((context) -> assertThat(context).doesNotHaveBean("beanEnabledByToxiProxyCondition"));
    }

    @Configuration(proxyBeanMethods = false)
    static class TestConfig {

        @Bean(name = BEAN)
        @ConditionalOnToxiProxyEnabled
        String beanEnabledByToxiProxyCondition() {
            return "beanEnabledByToxiProxyCondition";
        }

    }

}