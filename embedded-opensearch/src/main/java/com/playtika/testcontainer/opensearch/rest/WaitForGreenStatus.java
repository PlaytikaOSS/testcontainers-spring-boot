package com.playtika.testcontainer.opensearch.rest;

import com.playtika.testcontainer.common.checks.AbstractCommandWaitStrategy;
import com.playtika.testcontainer.opensearch.OpenSearchProperties;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class WaitForGreenStatus extends AbstractCommandWaitStrategy {

    private final OpenSearchProperties properties;

    @Override
    public String[] getCheckCommand() {
        List<String> command = new ArrayList<>(List.of(
                "curl", "-X", "GET", (properties.isCredentialsEnabled() ? "https" : "http") +
                        "://127.0.0.1:" + properties.getHttpPort() + "/_cluster/health?wait_for_status=green&timeout=10s"
        ));
        if (properties.isCredentialsEnabled()) {
            command.add("-u");
            command.add(properties.getUsername() + ':' + properties.getPassword());
            if (properties.isAllowInsecure()) {
                command.add("-k");
            }
        }
        return command.toArray(new String[0]);
    }
}
