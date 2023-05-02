package com.playtika.testcontainer.common.spring;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

public class NoDockerPresenceMarkerFailureAnalyser extends AbstractFailureAnalyzer<NoDockerPresenceMarkerException> {

    public static final String description = "No docker presence marker found in application context. Missing spring-cloud-starter?";
    public static final String action = "Follow the guide: https://github.com/testcontainers/testcontainers-spring-boot#how-to-use";

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, NoDockerPresenceMarkerException cause) {
        return new FailureAnalysis(description, action, cause);
    }
}
