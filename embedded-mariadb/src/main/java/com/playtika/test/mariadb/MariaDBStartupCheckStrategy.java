package com.playtika.test.mariadb;

import com.playtika.test.common.checks.AbstractStartupCheckStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MariaDBStartupCheckStrategy extends AbstractStartupCheckStrategy {

    @Override
    public String getContainerType() {
        return "MariaDB";
    }

    @Override
    public String[] getHealthCheckCmd() {
        return new String[] {
                "mysql",
                "-h",
                "127.0.0.1",
                "-e",
                "SELECT 1"

        };
    }
}
