/*
* The MIT License (MIT)
*
* Copyright (c) 2017 Playtika
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
* SOFTWARE.
 */
package com.playtika.test.aerospike;

import com.aerospike.client.AerospikeClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;

import java.util.*;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@Order(HIGHEST_PRECEDENCE)
@ConditionalOnProperty(value = "embedded.aerospike.enabled", matchIfMissing = true)
@ConditionalOnClass(AerospikeClient.class)
@Configuration
public class EmbeddedAerospikeAutoConfiguration {

    static final int AEROSPIKE_PORT = 3000;
    static final String NAMESPACE = "TEST";

    @Autowired
    ConfigurableEnvironment environment;

    @Bean
    @ConditionalOnMissingBean
    public AerospikeStartupCheckStrategy aerospikeStartupCheckStrategy() {
        return new AerospikeStartupCheckStrategy();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnMissingBean
    public GenericContainer aerosike(AerospikeStartupCheckStrategy aerospikeStartupCheckStrategy) {
        GenericContainer aerosike =
                new GenericContainer("aerospike:3.15.0.1")
                        .withStartupCheckStrategy(aerospikeStartupCheckStrategy)
                        .withExposedPorts(AEROSPIKE_PORT)
                        .withClasspathResourceMapping(
                                "aerospike.conf",
                                "/etc/aerospike/aerospike.conf",
                                BindMode.READ_ONLY);
        return aerosike;
    }

    @Bean
    @ConditionalOnMissingBean
    public EmbeddedAerospikeInfo embeddedAerospikeInfo(GenericContainer aerosike) {
        Integer mappedPort = aerosike.getMappedPort(AEROSPIKE_PORT);
        String host = aerosike.getContainerIpAddress();
        EmbeddedAerospikeInfo info = new EmbeddedAerospikeInfo(NAMESPACE, host, mappedPort);

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.aerospike.host", info.getHost());
        map.put("embedded.aerospike.port", info.getPort());
        map.put("embedded.aerospike.namespace", NAMESPACE);
        MapPropertySource propertySource = new MapPropertySource("embeddedAerospikeInfo", map);
        environment.getPropertySources().addFirst(propertySource);

        return info;
    }

    @ConditionalOnClass(AerospikeClient.class)
    @Configuration
    protected static class AerospikeBeanPostProcessor implements BeanFactoryPostProcessor {

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            setupDepends(beanFactory, AerospikeClient.class, "aerosike");
        }

        private void setupDepends(ConfigurableListableBeanFactory beanFactory, Class<?> beanClass, String dependsOnBean) {
            boolean includeNonSingletons = true;
            boolean allowEagerInit = true;
            String[] beanNamesForType = beanFactory.getBeanNamesForType(beanClass, includeNonSingletons, allowEagerInit);
            List<String> beansOfType = Arrays.asList(beanNamesForType);
            beansOfType.forEach(
                    beanName -> setupDependsOn(beanFactory, beanName, dependsOnBean)
            );
        }

        private void setupDependsOn(ConfigurableListableBeanFactory beanFactory, String beanName, String dependsOnBean) {
            BeanDefinition bean = beanFactory.getBeanDefinition(beanName);
            Set<String> dependsOn = new LinkedHashSet<>(asList(bean.getDependsOn()));
            dependsOn.add(dependsOnBean);
            bean.setDependsOn(dependsOn.toArray(new String[dependsOn.size()]));
        }

        private static List<String> asList(String[] array) {
            return (array == null ? Collections.emptyList() : Arrays.asList(array));
        }
    }
}
