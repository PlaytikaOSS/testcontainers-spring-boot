package com.playtika.testcontainer.selenium.testscope;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.TestContext;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.testcontainers.lifecycle.TestDescription;
import org.testcontainers.lifecycle.TestLifecycleAware;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TestcontainerScope implements Scope {
    public static final String NAME = "testcontainer";

    private static final String TEST_CONTAINER_CLASS = "org.testcontainers.containers.GenericContainer";

    private static final String[] BEAN_CLASSES = { TEST_CONTAINER_CLASS };

    private final Map<String, Object> instances = new HashMap<>();

    @Override
    public Object get(String name, ObjectFactory<?> objectFactory) {
        synchronized (this.instances) {
            Object instance = this.instances.get(name);
            if (instance == null) {
                instance = objectFactory.getObject();
                this.instances.put(name, instance);
            }
            return instance;
        }
    }

    @Override
    public Object remove(String name) {
        synchronized (this.instances) {
            return this.instances.remove(name);
        }
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback) {
    }

    @Override
    public Object resolveContextualObject(String key) {
        return null;
    }

    @Override
    public String getConversationId() {
        return null;
    }

    /**
     * Register this scope with the specified context and reassign appropriate bean
     * definitions to used it.
     * @param context the application context
     */
    static void registerWith(ConfigurableApplicationContext context) {
        if (!ClassUtils.isPresent(TEST_CONTAINER_CLASS, null)) {
            return;
        }
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        if (beanFactory.getRegisteredScope(NAME) == null) {
            beanFactory.registerScope(NAME, new TestcontainerScope());
        }
        context.addBeanFactoryPostProcessor(TestcontainerScope::postProcessBeanFactory);
    }

    private static void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        for (String beanClass : BEAN_CLASSES) {
            for (String beanName : beanFactory.getBeanNamesForType(ClassUtils.resolveClassName(beanClass, null))) {
                BeanDefinition definition = beanFactory.getBeanDefinition(beanName);
                if (!StringUtils.hasLength(definition.getScope())) {
                    definition.setScope(NAME);
                }
            }
        }
    }

    /**
     * Return the {@link TestcontainerScope} being used by the specified context (if any).
     * @param context the application context
     * @return the web driver scope or {@code null}
     */
    static TestcontainerScope getFrom(ApplicationContext context) {
        if (context instanceof ConfigurableApplicationContext) {
            Scope scope = ((ConfigurableApplicationContext) context).getBeanFactory().getRegisteredScope(NAME);
            return (scope instanceof TestcontainerScope) ? (TestcontainerScope) scope : null;
        }
        return null;
    }

    public void afterTestMethod(TestContext testContext) {
        instances.values().stream()
                .filter(TestLifecycleAware.class::isInstance)
                .map(TestLifecycleAware.class::cast)
                .forEach(value -> {
                    value.afterTest(testDescription(testContext), Optional.ofNullable(testContext.getTestException()));
                });
     }

    private TestDescription testDescription(TestContext testContext) {
        return new TestDescription() {

            @Override
            public String getTestId() {
                return getFilesystemFriendlyName();
            }

            @Override
            public String getFilesystemFriendlyName() {
                return testContext.getTestClass().getName() + "-" +  testContext.getTestMethod().getName();
            }
        };
    }

    public void beforeTestMethod(TestContext testContext) {
        instances.values().stream()
                .filter(TestLifecycleAware.class::isInstance)
                .map(TestLifecycleAware.class::cast)
                .forEach(value -> {
                    value.beforeTest(testDescription(testContext));
                });
    }
}