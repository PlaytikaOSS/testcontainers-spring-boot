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
package com.playtika.test.bootstrap;

import com.playtika.test.common.checks.PositiveCommandWaitStrategy;
import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
@AutoConfigureOrder
public class EmbeddedContainersBootstrapConfiguration {

    @Bean
    EchoContainer echoContainer1() {
        EchoContainer echoContainer = new EchoContainer()
                .withCommand("/bin/sh", "-c", "while true; do echo 'Press [CTRL+C] to stop..'; sleep 1; done")
                .waitingFor(new PositiveCommandWaitStrategy())
                .withStartupTimeout(Duration.ofSeconds(15));
        echoContainer.start();
        return echoContainer;
    }

    @Bean
    EchoContainer echoContainer2() {
        EchoContainer echoContainer = new EchoContainer()
                .withCommand("/bin/sh", "-c", "while true; do echo 'Press [CTRL+C] to stop..'; sleep 1; done")
                .waitingFor(new PositiveCommandWaitStrategy())
                .withStartupTimeout(Duration.ofSeconds(15));
        echoContainer.start();
        return echoContainer;
    }

    @Bean
    CommonContainerProperties properties(){
        CommonContainerProperties props = new CommonContainerProperties();
        props.setEnabled(true);
        return props;
    }
}
