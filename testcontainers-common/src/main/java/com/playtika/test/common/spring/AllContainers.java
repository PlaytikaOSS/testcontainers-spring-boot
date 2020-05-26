package com.playtika.test.common.spring;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.springframework.beans.factory.DisposableBean;
import org.testcontainers.containers.GenericContainer;

import java.util.List;

@Value
@AllArgsConstructor
public class AllContainers implements DisposableBean {

    List<GenericContainer> genericContainers;

    @Override
    public void destroy(){
        genericContainers.parallelStream()
                .forEach(GenericContainer::stop);
    }

    public boolean isEmpty(){
        return genericContainers.size() == 0;
    }
}
