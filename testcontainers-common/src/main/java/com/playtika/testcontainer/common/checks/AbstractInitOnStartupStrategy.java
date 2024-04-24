package com.playtika.testcontainer.common.checks;

public abstract class AbstractInitOnStartupStrategy extends AbstractCommandWaitStrategy {

    private volatile boolean wasExecutedOnce;

    public abstract String[] getScriptToExecute();

    @Override
    public String[] getCheckCommand() {
        return getScriptToExecute();
    }

    @Override
    protected void waitUntilReady() {
        if (wasExecutedOnce) {
            return;
        }
        super.waitUntilReady();
        wasExecutedOnce = true;
    }
}
