package com.playtika.test.elasticsearch.springdata;

import com.playtika.test.elasticsearch.ElasticSearchProperties;
import com.playtika.test.elasticsearch.EmbeddedElasticSearchBootstrapConfigurationTest;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class SpringDataTest extends EmbeddedElasticSearchBootstrapConfigurationTest {

    @Autowired
    private TestDocumentRepository documentRepository;

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

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
        String[] beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, RestClient.class);

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
}
