package com.playtika.test.memsql;

import com.playtika.test.common.checks.AbstractCommandWaitStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MemSqlStatusCheck extends AbstractCommandWaitStrategy {

    @Override
    public String[] getCheckCommand() {
        return new String[]{
                "mysql",
                "-u",
                "root",
                "-h",
                "127.0.0.1",
                "-P",
                "3306",
                "--batch",
                "-e",
                "use test_db; SELECT 1;"
        };
    }
}
