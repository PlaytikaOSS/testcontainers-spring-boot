/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Playtika
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.playtika.test.common.spring;

import com.playtika.test.common.properties.TestcontainersProperties;
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
