package com.playtika.test.mongodb;

import com.playtika.test.common.checks.AbstractCommandWaitStrategy;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MongodbStatusCheck extends AbstractCommandWaitStrategy {
    private final MongodbProperties properties;

    @Override
    public String[] getCheckCommand() {
        if (properties.getCheckCommand().length == 0) {
            return new String[]{"mongo", "admin", "--eval", "\"db['system.version'].find()\""};
        } else {
            return properties.getCheckCommand();
        }
    }
}
