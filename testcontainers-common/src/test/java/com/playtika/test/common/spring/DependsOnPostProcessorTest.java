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

import static com.playtika.test.common.spring.EmbeddedContainersShutdownAutoConfiguration.ALL_CONTAINERS;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

@ExtendWith(MockitoExtension.class)
public class DependsOnPostProcessorTest {

    private static final String TEST_BEAN_NAME_3 = "testBean3";
    private static final String TEST_BEAN_NAME_4 = "testBean4";
    private static final String TEST_BEAN_NAME_1 = "testBean1";
    private static final String TEST_BEAN_NAME_2 = "testBean2";
    private static final String TEST_PROCESSOR_BEAN = "testProcessorBean";

    private DependsOnPostProcessor dependsOnPostProcessor;

    @Mock
    private ConfigurableListableBeanFactory configurableListableBeanFactory;
    @Mock
    private BeanDefinition beanDefinition;

    @BeforeEach
    public void setUp() {
        dependsOnPostProcessor = new DependsOnPostProcessor(TestProcessor.class, new String[]{TEST_BEAN_NAME_3, TEST_BEAN_NAME_4});
    }

    @Test
    public void shouldExtendListOfBeanDependenciesIfBeanDefinitionAlreadyContainsDependencies() {
        when(configurableListableBeanFactory.getBeanNamesForType(TestProcessor.class, true, true)).thenReturn(new String[]{TEST_PROCESSOR_BEAN});
        when(configurableListableBeanFactory.getBeanDefinition(TEST_PROCESSOR_BEAN)).thenReturn(beanDefinition);
        when(beanDefinition.getDependsOn()).thenReturn(new String[]{TEST_BEAN_NAME_1, TEST_BEAN_NAME_2});

        dependsOnPostProcessor.postProcessBeanFactory(configurableListableBeanFactory);

        String[] expected = {TEST_BEAN_NAME_1, TEST_BEAN_NAME_2, TEST_BEAN_NAME_3, TEST_BEAN_NAME_4, ALL_CONTAINERS};

        verify(beanDefinition, times(1)).setDependsOn(expected);
    }

    @Test
    public void shouldExtendListOfBeanDependenciesIfBeanDefinitionDoesNotContainDependencies() {
        when(configurableListableBeanFactory.getBeanNamesForType(TestProcessor.class, true, true)).thenReturn(new String[]{TEST_PROCESSOR_BEAN});
        when(configurableListableBeanFactory.getBeanDefinition(TEST_PROCESSOR_BEAN)).thenReturn(beanDefinition);
        when(beanDefinition.getDependsOn()).thenReturn(null);

        dependsOnPostProcessor.postProcessBeanFactory(configurableListableBeanFactory);

        String[] expected = {TEST_BEAN_NAME_3, TEST_BEAN_NAME_4, ALL_CONTAINERS};

        verify(beanDefinition, times(1)).setDependsOn(expected);
    }

    private static class TestProcessor {
    }
}