package com.playtika.testcontainer.opensearch.springdata;

import com.playtika.testcontainer.opensearch.EmbeddedOpenSearchBootstrapConfigurationTest;
import com.playtika.testcontainer.opensearch.OpenSearchProperties;
import org.junit.jupiter.api.Test;
import org.opensearch.client.RestClient;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("enabled")
@SpringBootTest(
        classes = EmbeddedOpenSearchBootstrapConfigurationTest.Config.class,
        properties = {
                "embedded.opensearch.enabled=true"
        })
public class SpringDataTest extends EmbeddedOpenSearchBootstrapConfigurationTest {

    @Autowired
    protected TestDocumentRepository documentRepository;

    @Autowired
    protected ConfigurableListableBeanFactory beanFactory;

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
                    .contains("opensearchRestClient");
            asList(beanNamesForType).forEach(this::hasDependsOn);
        }
    }

    private void hasDependsOn(String beanName) {
        assertThat(beanFactory.getBeanDefinition(beanName).getDependsOn())
                .isNotNull()
                .isNotEmpty()
                .contains(OpenSearchProperties.BEAN_NAME_EMBEDDED_OPEN_SEARCH);
    }

    private TestDocument saveDocument(String key, String value) {
        TestDocument testDocument = new TestDocument(key, value);
        documentRepository.save(testDocument);
        return testDocument;
    }
}
