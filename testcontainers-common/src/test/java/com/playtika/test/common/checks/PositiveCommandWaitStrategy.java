package com.playtika.test.common.checks;

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
