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
package com.playtika.test.mongodb;


import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.client.MongoDatabase;

@SpringBootTest(
        properties = {
                "embedded.mongodb.install.enabled=true",
                "embedded.mongodb.config-file=config-replset.yaml"
        }
        ,classes = EmbeddedMongodbBootstrapWithConfigFileTest.TestConfiguration.class
)
public class EmbeddedMongodbBootstrapWithConfigFileTest {

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    ConfigurableEnvironment environment;

    @Test
    public void shouldSetupReplset() {
    	/*
    	 * The config file set a replica set name, so getting
    	 * status should indicate that replicaset has been set.
    	 */
		MongoDatabase adminDb = mongoTemplate.getMongoDbFactory().getMongoDatabase("admin");
		Document stats = new Document("serverStatus", 1);
		stats.append("repl", 1);
		Document result = adminDb.runCommand(new Document("serverStatus", stats));
		Document replSet = result.get("repl", Document.class);
		assertNotNull(replSet);
		assertTrue(replSet.getBoolean("isreplicaset"));
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}
