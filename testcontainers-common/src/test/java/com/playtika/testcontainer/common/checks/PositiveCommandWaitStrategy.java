package com.playtika.testcontainer.common.checks;

public class PositiveCommandWaitStrategy extends AbstractCommandWaitStrategy {

    @Override
    public String getContainerType() {
        return "Positive Test";
    }

    @Override
    public String[] getCheckCommand() {
        return new String[]{"echo", "health check passed"};
    }
}
