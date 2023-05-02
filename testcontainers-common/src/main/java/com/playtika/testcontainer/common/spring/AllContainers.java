package com.playtika.testcontainer.common.spring;

import com.playtika.testcontainer.common.properties.TestcontainersProperties;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.testcontainers.containers.GenericContainer;

import java.util.List;

@Value
@Slf4j
public class AllContainers implements DisposableBean {

    List<GenericContainer> genericContainers;
    TestcontainersProperties testcontainersProperties;

    public AllContainers(List<GenericContainer> genericContainers, TestcontainersProperties testcontainersProperties) {
        this.genericContainers = genericContainers;
        this.testcontainersProperties = testcontainersProperties;
    }

    @Override
    public void destroy() {
        if(testcontainersProperties.isForceShutdown()) {
            genericContainers.parallelStream()
                    .forEach(GenericContainer::stop);
        }
    }
}
