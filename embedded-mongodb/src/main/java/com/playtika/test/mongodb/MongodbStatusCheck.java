package com.playtika.test.mongodb;

import com.playtika.test.common.checks.AbstractCommandWaitStrategy;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MongodbStatusCheck extends AbstractCommandWaitStrategy {
    private static final String COMMAND = "\"db['system.version'].find()\"";
    private final MongodbProperties properties;

    @Override
    public String[] getCheckCommand() {
        if (properties.getUsername() == null) {
            return new String[]{properties.getShell(), "--eval", COMMAND};
        }

        return new String[]{
                properties.getShell(), "admin",
                "-u", properties.getUsername(),
                "-p", properties.getPassword(),
                "--eval", COMMAND
        };
    }
}
