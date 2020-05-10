/*
* The MIT License (MIT)
*
* Copyright (c) 2020 Playtika
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
package com.playtika.test.common.spring;

import lombok.AllArgsConstructor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.playtika.test.common.spring.EmbeddedContainersShutdownAutoConfiguration.ALL_CONTAINERS;

@AllArgsConstructor
public class DependsOnPostProcessor implements BeanFactoryPostProcessor {

    final Class<?> beansOfType;
    final String[] dependsOn;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        List<String> beanNamesForType = asList(getBeanNamesForType(beanFactory));
        beanNamesForType.forEach(
                datastoreClientBeanName -> {
                    setupDependsOn(beanFactory, datastoreClientBeanName);
                }
        );
    }

    private void setupDependsOn(ConfigurableListableBeanFactory beanFactory, String dataSourceBeanName) {
        BeanDefinition dataStoreClientBeanDefinition = beanFactory.getBeanDefinition(dataSourceBeanName);
        List<String> dependsOnExisting = asList(dataStoreClientBeanDefinition.getDependsOn());
        dependsOnExisting.addAll(asList(dependsOn));
        dependsOnExisting.add(ALL_CONTAINERS);
        dataStoreClientBeanDefinition.setDependsOn(dependsOnExisting.toArray(new String[]{}));
    }

    private String[] getBeanNamesForType(ConfigurableListableBeanFactory beanFactory) {
        boolean includeNonSingletons = true;
        boolean allowEagerInit = true;
        return beanFactory.getBeanNamesForType(beansOfType, includeNonSingletons, allowEagerInit);
    }

    private static List<String> asList(String[] array) {
        return (array == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(array)));
    }
}
