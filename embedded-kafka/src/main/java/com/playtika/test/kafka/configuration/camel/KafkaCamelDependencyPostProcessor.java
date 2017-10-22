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
package com.playtika.test.kafka.configuration.camel;

import org.apache.camel.CamelContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.*;

public class KafkaCamelDependencyPostProcessor implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        List<String> camelContexts = asList(beanFactory
                .getBeanNamesForType(CamelContext.class, true, true));
        camelContexts.forEach(
                beanName -> setupDependsOn(beanFactory, beanName)
        );
    }

    private void setupDependsOn(ConfigurableListableBeanFactory beanFactory, String beanName) {
        BeanDefinition bean = beanFactory.getBeanDefinition(beanName);
        Set<String> dependsOn = new LinkedHashSet<String>(asList(bean.getDependsOn()));
        dependsOn.add("kafka");
        dependsOn.add("zookeeper");
        bean.setDependsOn(dependsOn.toArray(new String[dependsOn.size()]));
    }

    private List<String> asList(String[] array) {
        return (array == null ? Collections.<String>emptyList() : Arrays.asList(array));
    }
}