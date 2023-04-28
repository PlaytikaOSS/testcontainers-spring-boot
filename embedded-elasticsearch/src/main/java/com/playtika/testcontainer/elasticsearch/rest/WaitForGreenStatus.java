package com.playtika.testcontainer.elasticsearch.rest;

import com.playtika.testcontainer.common.checks.AbstractCommandWaitStrategy;
import com.playtika.testcontainer.elasticsearch.ElasticSearchProperties;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WaitForGreenStatus extends AbstractCommandWaitStrategy {

    private final ElasticSearchProperties properties;

    @Override
    public String[] getCheckCommand() {
        return new String[]{
            "curl", "-X", "GET",
            "http://127.0.0.1:" + properties.getHttpPort() + "/_cluster/health?wait_for_status=green&timeout=10s"
        };
    }
}
