package com.playtika.testcontainer.opensearch.rest;

import com.playtika.testcontainer.common.checks.AbstractInitOnStartupStrategy;
import com.playtika.testcontainer.opensearch.OpenSearchProperties;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class CreateIndex extends AbstractInitOnStartupStrategy {

    private final OpenSearchProperties properties;
    private final String index;

    @Override
    public String[] getScriptToExecute() {
        List<String> command = new ArrayList<>(List.of(
                "curl", "-X", "PUT", (properties.isCredentialsEnabled() ? "https" : "http") +
                        "://127.0.0.1:" + properties.getHttpPort() + "/" + index
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
