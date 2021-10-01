package com.playtika.test.voltdb;

import com.playtika.test.common.checks.AbstractCommandWaitStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class VoltDBStatusCheck extends AbstractCommandWaitStrategy {

    @Override
    public String[] getCheckCommand() {
        return new String[] {
                "bin/sqlcmd",
                "--query=exec @Ping;"
        };
    }
}
