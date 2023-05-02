package com.playtika.testcontainer.couchbase.springdata;

import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.playtika.testcontainer.couchbase.EmbeddedCouchbaseBootstrapConfigurationTest;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.data.couchbase.core.CouchbaseOperations;
import org.testcontainers.containers.ToxiproxyContainer;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.playtika.testcontainer.couchbase.CouchbaseProperties.BEAN_NAME_EMBEDDED_COUCHBASE;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class SpringDataTest extends EmbeddedCouchbaseBootstrapConfigurationTest {

    @Autowired
    TestDocumentRepository documentRepository;

    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @Autowired
    ToxiproxyContainer.ContainerProxy couchbaseContainerProxy;

    @Autowired
    CouchbaseOperations couchbaseOperations;

    @Autowired
    CouchbaseConfigurationProperties couchbaseConfigurationProperties;

    @Test
    public void springDataShouldWork() {
        String key = "test::1";
        String value = "myvalue";
        assertThat(documentRepository).isNotNull();
        assertThat(documentRepository.existsById(key)).isFalse();

        TestDocument testDocument = saveDocument(key, value);

        assertThat(documentRepository.findById(key).get()).isEqualTo(testDocument);
    }

    @Test
    @Disabled
    public void shouldEmulateNetworkLatency() throws Exception {
        couchbaseContainerProxy.toxics().timeout("timeout", ToxicDirection.DOWNSTREAM, 1000);

        assertThat(durationOf(() -> documentRepository.existsById("abc")))
                .isCloseTo(1000L, Offset.offset(200L));

        couchbaseContainerProxy.toxics().get("timeout").remove();

        assertThat(durationOf(() -> documentRepository.existsById("abc")))
                .isLessThan(100L);
    }

    @Test
    public void n1q1ShouldWork() {
        String title = "some query title";
        saveDocument("test::2", "custom value");
        saveDocument("test::3", title);
        saveDocument("test::4", title);

        await().atMost(60, TimeUnit.SECONDS).until(() -> {
            List<TestDocument> resultList = documentRepository.findByTitle(title);
            assertThat(resultList.size()).isEqualTo(2);
            return true;
        });
    }

    @Test
    public void shouldSetupDependsOnForNewClient() {
        String[] beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, Bucket.class);
        if (beanNamesForType.length > 0) {
            assertThat(beanNamesForType)
                    .as("New sync client should be present")
                    .hasSize(1)
                    .contains("couchbaseBucket");
            asList(beanNamesForType).forEach(this::hasDependsOn);
        }

        beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, AsyncBucket.class);
        if (beanNamesForType.length > 0) {
            assertThat(beanNamesForType)
                    .as("New async client should be present")
                    .hasSize(1)
                    .contains("asyncCouchbaseBucket");
            asList(beanNamesForType).forEach(this::hasDependsOn);
        }

        beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, Cluster.class);

        if (beanNamesForType.length > 0) {
            assertThat(beanNamesForType)
                    .as("New async client should be present")
                    .hasSize(1)
                    .contains("couchbaseCluster");
            asList(beanNamesForType).forEach(this::hasDependsOn);
        }
    }

    private void hasDependsOn(String beanName) {
        assertThat(beanFactory.getBeanDefinition(beanName).getDependsOn())
                .isNotNull()
                .isNotEmpty()
                .contains(BEAN_NAME_EMBEDDED_COUCHBASE);
    }

    private TestDocument saveDocument(String key, String value) {
        TestDocument testDocument = TestDocument.builder()
                .key(key)
                .title(value)
                .build();
        documentRepository.save(testDocument);
        return testDocument;
    }

    private static long durationOf(Callable<?> op) throws Exception {
        long start = System.currentTimeMillis();
        op.call();
        return System.currentTimeMillis() - start;
    }
}
