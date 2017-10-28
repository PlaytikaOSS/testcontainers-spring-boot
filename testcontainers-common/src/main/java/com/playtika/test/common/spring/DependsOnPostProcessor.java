package com.playtika.test.common.spring;

import lombok.AllArgsConstructor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
public class DependsOnPostProcessor implements BeanFactoryPostProcessor {

    final Class<?> beansOfType;
    final String[] dependsOn;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        List<String> beanNamesForType = asList(getBeanNamesForType(beanFactory));
        beanNamesForType.forEach(
                dataSourceBeanName -> {
                    setupDependsOn(beanFactory, dataSourceBeanName);
                }
        );
    }

    private void setupDependsOn(ConfigurableListableBeanFactory beanFactory, String dataSourceBeanName) {
        BeanDefinition dataSourceBeanDefinition = beanFactory.getBeanDefinition(dataSourceBeanName);
        List<String> dependsOnExisting = asList(dataSourceBeanDefinition.getDependsOn());
        dependsOnExisting.addAll(asList(dependsOn));
        dataSourceBeanDefinition.setDependsOn(dependsOnExisting.toArray(new String[]{}));
    }

    private String[] getBeanNamesForType(ConfigurableListableBeanFactory beanFactory) {
        boolean includeNonSingletons = true;
        boolean allowEagerInit = true;
        return beanFactory.getBeanNamesForType(beansOfType, includeNonSingletons, allowEagerInit);
    }

    private static List<String> asList(String[] array) {
        return (array == null ? new ArrayList<>() : Arrays.asList(array));
    }
}
