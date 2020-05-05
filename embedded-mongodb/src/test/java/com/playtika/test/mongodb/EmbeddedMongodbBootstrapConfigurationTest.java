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
package com.playtika.test.mongodb;

import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Callable;

import com.playtika.test.common.operations.NetworkTestOperations;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoTemplate;

@Slf4j
@SpringBootTest(
        properties = {
                "embedded.mongodb.install.enabled=true",
                "spring.data.mongodb.uri=mongodb://${embedded.mongodb.host}:${embedded.mongodb.port}/${embedded.mongodb.database}"
        })
public class EmbeddedMongodbBootstrapConfigurationTest {

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    ConfigurableEnvironment environment;

    @Autowired
    NetworkTestOperations mongodbNetworkTestOperations;

    @Test
    public void shouldSaveAndGet() {
        String someId = UUID.randomUUID().toString();
        Foo foo = new Foo(someId, "foo", Instant.parse("2019-09-26T07:57:12.801Z"), -42L);
        mongoTemplate.save(foo);

        assertThat(mongoTemplate.findById(someId, Foo.class)).isEqualTo(foo);
    }

    @Test
    public void shouldEmulateLatency() throws Exception {
        mongodbNetworkTestOperations.withNetworkLatency(ofMillis(1000),
                () -> assertThat(durationOf(() -> mongoTemplate.findById("any", Foo.class)))
                        .isCloseTo(1000L, Offset.offset(100L))
        );

        assertThat(durationOf(() -> mongoTemplate.findById("any", Foo.class)))
                .isLessThan(100L);
    }

    @Test
    public void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.mongodb.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.mongodb.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.mongodb.database")).isNotEmpty();
    }

    private static long durationOf(Callable<?> op) throws Exception {
        long start = System.currentTimeMillis();
        op.call();
        return System.currentTimeMillis() - start;
    }

    @Value
    static class Foo {
        @Id
        String someId;
        String someString;
        Instant someTimestamp;
        Long someNumber;
    }
}
