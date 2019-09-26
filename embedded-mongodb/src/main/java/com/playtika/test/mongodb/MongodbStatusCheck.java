package com.playtika.test.mongodb;

import com.playtika.test.common.checks.AbstractCommandWaitStrategy;

public class MongodbStatusCheck extends AbstractCommandWaitStrategy {

    @Override
    public String[] getCheckCommand() {
        return new String[]{"mongo", "admin", "--eval", "\"db['system.version'].find()\""};
    }
}
