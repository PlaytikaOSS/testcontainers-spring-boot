package com.playtika.test.common.spring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.GenericContainer;

import java.util.Arrays;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AllContainersTest {

    @Mock
    GenericContainer c1;
    @Mock
    GenericContainer c2;

    @Test
    void testContainersStoppedOnDestroy() {

        AllContainers containers = new AllContainers(Arrays.asList(c1, c2));
        containers.destroy();

        verify(c1, times(1)).stop();
        verify(c2, times(1)).stop();
    }
}