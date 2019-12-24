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
package com.playtika.test.elasticsearch.springdata;

import com.playtika.test.common.operations.NetworkTestOperations;
import com.playtika.test.elasticsearch.ElasticSearchProperties;
import com.playtika.test.elasticsearch.EmbeddedElasticSearchBootstrapConfigurationTest;
import org.assertj.core.data.Offset;
import org.elasticsearch.client.Client;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.List;
import java.util.concurrent.Callable;

import static java.time.Duration.ofMillis;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class SpringDataTest extends EmbeddedElasticSearchBootstrapConfigurationTest {

    @Autowired
    private TestDocumentRepository documentRepository;

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Autowired
    private NetworkTestOperations elasticSearchNetworkTestOperations;

    @Test
    public void springDataShouldWork() {
        String key = "test::1";
        String value = "myvalue";
        assertThat(documentRepository).isNotNull();
        assertThat(documentRepository.findById(key).isPresent()).isFalse();

        TestDocument testDocument = saveDocument(key, value);

        assertThat(documentRepository.findById(key).get()).isEqualTo(testDocument);
    }

    @Test
    public void shouldEmulateNetworkLatency() throws Exception {
        elasticSearchNetworkTestOperations.withNetworkLatency(ofMillis(1000),
                () -> assertThat(durationOf(() -> documentRepository.findById("abc")))
                        .isCloseTo(1000L, Offset.offset(100L))
        );

        assertThat(durationOf(() -> documentRepository.findById("abc")))
                .isLessThan(40L);
    }

    @Test
    public void queryShouldWork() {
        String title = "some query title";
        saveDocument("test::2", "custom value");
        saveDocument("test::3", title);
        saveDocument("test::4", title);

        List<TestDocument> resultList = documentRepository.findByTitle(title);

        assertThat(resultList.size()).isEqualTo(2);
    }

    @Test
    public void shouldSetupDependsOnForNewClient() {
        String[] beanNamesForType = beanFactory.getBeanNamesForType(Client.class);
        assertThat(beanNamesForType)
            .as("New client should be present")
            .hasSize(1)
            .contains("elasticsearchClient");
        asList(beanNamesForType).forEach(this::hasDependsOn);
    }

    private void hasDependsOn(String beanName) {
        assertThat(beanFactory.getBeanDefinition(beanName).getDependsOn())
            .isNotNull()
            .isNotEmpty()
            .contains(ElasticSearchProperties.BEAN_NAME_EMBEDDED_ELASTIC_SEARCH);
    }

    private TestDocument saveDocument(String key, String value) {
        TestDocument testDocument = new TestDocument(key, value);
        documentRepository.save(testDocument);
        return testDocument;
    }

    private static long durationOf(Callable<?> op) throws Exception {
        long start = System.currentTimeMillis();
        op.call();
        return System.currentTimeMillis() - start;
    }
}
