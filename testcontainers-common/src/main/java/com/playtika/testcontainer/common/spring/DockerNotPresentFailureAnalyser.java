package com.playtika.testcontainer.common.spring;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

public class DockerNotPresentFailureAnalyser extends AbstractFailureAnalyzer<DockerNotPresentException> {

    public static final String description = "No docker daemon available. Forgot to install/start docker?";
    public static final String action = "Follow the guide: https://docs.docker.com/get-docker/";

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, DockerNotPresentException cause) {
        return new FailureAnalysis(description, action, cause);
    }
}
