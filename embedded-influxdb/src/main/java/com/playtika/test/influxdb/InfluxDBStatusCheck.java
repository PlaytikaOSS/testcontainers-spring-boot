package com.playtika.test.influxdb;

import com.playtika.test.common.checks.AbstractCommandWaitStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class InfluxDBStatusCheck extends AbstractCommandWaitStrategy {

    private final InfluxDBProperties properties;

    @Override
    public String[] getCheckCommand() {
        return new String[]{
                "influx",
                "-username",
                properties.getUser(),
                "-password",
                properties.getPassword(),
                "-database",
                properties.getDatabase(),
                "-execute",
                "SHOW measurements"
        };
    }

}
