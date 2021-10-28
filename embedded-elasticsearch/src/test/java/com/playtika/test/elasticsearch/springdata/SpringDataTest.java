package com.playtika.test.elasticsearch.springdata;

import com.playtika.test.common.operations.NetworkTestOperations;
import com.playtika.test.elasticsearch.ElasticSearchProperties;
import com.playtika.test.elasticsearch.EmbeddedElasticSearchBootstrapConfigurationTest;
import org.assertj.core.data.Offset;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactoryUtils;
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
    private NetworkTestOperations elasticsearchNetworkTestOperations;

    @Test
    public void springDataShouldWork() {
        String key = "test::1";
        String value = "myvalue";
        assertThat(documentRepository).isNotNull();
        assertThat(documentRepository.findById(key).isPresent()).isFalse();

        TestDocument testDocument = saveDocument(key, value);

        assertThat(documentRepository.findById(key).get()).isEqualTo(testDocument);
    }

    //TODO: Need to figure out how to test this properly
    @Test
    @Disabled("TODO: Need to figure out how to test this properly")
    public void shouldEmulateNetworkLatency() throws Exception {
        elasticsearchNetworkTestOperations.withNetworkLatency(ofMillis(1000),
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
        String[] beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, Client.class);
        if (beanNamesForType.length > 0) {
            assertThat(beanNamesForType)
                    .as("New client should be present")
                    .hasSize(1)
                    .contains("elasticsearchClient");
            asList(beanNamesForType).forEach(this::hasDependsOn);
        }
        beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, RestClient.class);

        if (beanNamesForType.length > 0) {
            assertThat(beanNamesForType)
                    .as("New client should be present")
                    .hasSize(1)
                    .contains("elasticsearchRestClient");
            asList(beanNamesForType).forEach(this::hasDependsOn);
        }
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
