/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Playtika
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
package com.playtika.test.aerospike;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.policy.ClientPolicy;
import com.aerospike.client.policy.WritePolicy;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = BaseAerospikeTest.TestConfiguration.class)
public abstract class BaseAerospikeTest {

    protected static final String SET = "values";

    @Value("${embedded.aerospike.namespace}")
    protected String namespace;

    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @Autowired
    AerospikeClient client;

    @Autowired
    WritePolicy policy;

    @Autowired
    AerospikeTestOperations aerospikeTestOperations;

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {

        @Value("${embedded.aerospike.host}")
        String host;
        @Value("${embedded.aerospike.port}")
        int port;

        @Bean(destroyMethod = "close")
        public AerospikeClient aerospikeClient() {
            ClientPolicy clientPolicy = new ClientPolicy();
            clientPolicy.timeout = 10_000;//in millis
            return new AerospikeClient(clientPolicy, host, port);
        }

        @Bean
        public WritePolicy policy() {
            WritePolicy policy = new WritePolicy();
            policy.totalTimeout = 200;//in millis
            return policy;
        }
    }
}
