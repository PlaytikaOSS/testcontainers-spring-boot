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
package com.playtika.test.kafka;

import com.playtika.test.kafka.properties.KafkaConfigurationProperties;
import com.playtika.test.kafka.properties.ZookeeperConfigurationProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        properties = {
                "embedded.zookeeper.fileSystemBind.enabled=true",
                "embedded.zookeeper.fileSystemBind.dataFolder=target/embedded-zk-data-with-binding",
                "embedded.zookeeper.fileSystemBind.txnLogsFolder=target/embedded-zk-txn-logs-with-binding",

                "embedded.kafka.topicsToCreate=autoCreatedTopic",
                "embedded.kafka.fileSystemBind.enabled=true",
                "embedded.kafka.fileSystemBind.dataFolder=target/embedded-kafka-data-with-binding"
        },
        classes = BaseEmbeddedKafkaTest.TestConfiguration.class
)
@TestInstance(Lifecycle.PER_CLASS)
@DisplayName("Test that embedded-kafka with filesystem binding")
public class EmbeddedKafkaTests extends BaseEmbeddedKafkaTest {

    @Autowired
    private ZookeeperConfigurationProperties zookeeperProperties;

    @Autowired
    private KafkaConfigurationProperties kafkaProperties;

    @AfterAll
    public void shouldBindToFileSystem() {
        Path projectDir = projectDir();
        Path zookeeperDataFolder = projectDir.resolve(zookeeperProperties.getFileSystemBind().getDataFolder());
        Path zookeeperTxnLogsFolder = projectDir.resolve(zookeeperProperties.getFileSystemBind().getTxnLogsFolder());
        Path kafkaDataFolder = projectDir.resolve(kafkaProperties.getFileSystemBind().getDataFolder());

        assertThat(zookeeperDataFolder.toFile())
                .isDirectory()
                .isNotEmptyDirectory();
        assertThat(zookeeperTxnLogsFolder.toFile())
                .isDirectory()
                .isNotEmptyDirectory();
        assertThat(kafkaDataFolder.toFile())
                .isDirectory()
                .isNotEmptyDirectory();
    }
}