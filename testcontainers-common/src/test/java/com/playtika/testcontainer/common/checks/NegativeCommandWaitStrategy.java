package com.playtika.testcontainer.common.checks;

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
