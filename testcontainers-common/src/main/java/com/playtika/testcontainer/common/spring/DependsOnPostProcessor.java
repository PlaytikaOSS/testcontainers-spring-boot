package com.playtika.testcontainer.common.spring;

import java.util.Collections;
import java.util.List;

import static com.playtika.testcontainer.common.spring.EmbeddedContainersShutdownAutoConfiguration.ALL_CONTAINERS;

public class DependsOnPostProcessor extends AbstractDependsOnPostProcessor {

    public DependsOnPostProcessor(Class<?> beansOfType, String[] dependsOn) {
        super(beansOfType, dependsOn);
    }

    @Override
    protected List<String> getDefaultDependsOn() {
        return Collections.singletonList(ALL_CONTAINERS);
    }

}
