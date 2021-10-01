package com.playtika.test.common.checks;

public class NegativeCommandWaitStrategy extends AbstractCommandWaitStrategy {

    @Override
    public String getContainerType() {
        return "Negative Test";
    }

    @Override
    public String[] getCheckCommand() {
        return new String[]{"/bin/sh", "-c", "invalid command"};
    }
}
