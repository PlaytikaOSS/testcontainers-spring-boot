package com.playtika.test.couchbase.rest;

import com.playtika.test.common.checks.AbstractInitOnStartupStrategy;
import com.playtika.test.couchbase.CouchbaseProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

//Used for N1QL
@Slf4j
@RequiredArgsConstructor
public class CreatePrimaryIndex extends AbstractInitOnStartupStrategy {

    private final CouchbaseProperties properties;

    @Override
    public String[] getScriptToExecute() {
        return new String[]{
                "curl", "-X", "POST",
                "-u", properties.getCredentials(),
                "http://127.0.0.1:8093/query?statement=CREATE+PRIMARY+INDEX+ON+" + properties.getBucket()
        };
    }

}
