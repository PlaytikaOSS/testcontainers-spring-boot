package com.playtika.testcontainer.elasticsearch.rest;

import com.playtika.testcontainer.common.checks.AbstractInitOnStartupStrategy;
import com.playtika.testcontainer.elasticsearch.ElasticSearchProperties;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateIndex extends AbstractInitOnStartupStrategy {

    private final ElasticSearchProperties properties;
    private final String index;

    @Override
    public String[] getScriptToExecute() {
        return new String[]{
            "curl", "-X", "PUT",
            "http://127.0.0.1:" + properties.getHttpPort() + "/" + index
        };
    }
}
