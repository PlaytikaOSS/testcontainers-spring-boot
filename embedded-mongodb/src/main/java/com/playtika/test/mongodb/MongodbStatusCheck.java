package com.playtika.test.mongodb;

import com.playtika.test.common.checks.AbstractCommandWaitStrategy;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MongodbStatusCheck extends AbstractCommandWaitStrategy {
    private final MongodbProperties properties;

    @Override
    public String[] getCheckCommand() {
        String shell = properties.getShell();
        if (shell == null) {
            shell = "mongo";
        }
        return new String[]{
                shell, "admin",
                "-u", properties.getUsername(),
                "-p", properties.getPassword(),
                "--eval", "\"db['system.version'].find()\""
        };
    }
}
