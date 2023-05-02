package com.playtika.testcontainer.selenium.testscope;

import org.springframework.core.Ordered;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

public class TestcontainerTestExecutionListener extends AbstractTestExecutionListener {

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 100;
    }

    @Override
    public void beforeTestMethod(TestContext testContext) {
        TestcontainerScope scope = TestcontainerScope.getFrom(testContext.getApplicationContext());
        scope.beforeTestMethod(testContext);
    }

    @Override
    public void afterTestMethod(TestContext testContext) {
        TestcontainerScope scope = TestcontainerScope.getFrom(testContext.getApplicationContext());
        scope.afterTestMethod(testContext);
        if (scope != null ) {
            testContext.setAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE,
                    Boolean.TRUE);
        }
    }
}