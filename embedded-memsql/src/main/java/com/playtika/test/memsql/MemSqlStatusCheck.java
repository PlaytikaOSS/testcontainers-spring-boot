package com.playtika.test.memsql;

import com.playtika.test.common.checks.AbstractCommandWaitStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MemSqlStatusCheck extends AbstractCommandWaitStrategy {

    private final MemSqlProperties properties;

    @Override
    public String[] getCheckCommand() {
        return new String[]{
                "memsql",
                "-u" + properties.getUser(),
                "-p" + properties.getPassword(),
                "-h", properties.getHost(),
                "-P", String.valueOf(properties.getPort()),
                "-e", properties.getStatusCheck()
        };
    }
}
