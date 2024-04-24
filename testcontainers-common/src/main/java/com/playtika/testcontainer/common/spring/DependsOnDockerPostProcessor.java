package com.playtika.testcontainer.common.spring;

import java.util.Collections;
import java.util.List;

import static com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration.DOCKER_IS_AVAILABLE;

public class DependsOnDockerPostProcessor extends AbstractDependsOnPostProcessor {

    public DependsOnDockerPostProcessor(Class<?> beansOfType) {
        super(beansOfType, DOCKER_IS_AVAILABLE);
    }

    @Override
    protected List<String> getDefaultDependsOn() {
        return Collections.emptyList();
    }
}
