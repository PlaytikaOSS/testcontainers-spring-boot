package com.playtika.test.couchbase;

import com.couchbase.client.CouchbaseClient;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EmbeddedCouchbaseAutoConfigurationTest.SpringBootApp.class)
public class EmbeddedCouchbaseAutoConfigurationTest {

    public static final String KEY = "test::1";
    public static final String VALUE = "myvalue";

    @Autowired
    ConfigurableEnvironment environment;

    @Autowired
    TestDocumentRepository documentRepository;

    @Autowired
    CouchbaseClient couchbaseClient;

    @Test
    public void springDataShouldWork() throws Exception {

        assertThat(documentRepository).isNotNull();
        assertThat(documentRepository.findOne(KEY)).isNull();

        TestDocument testDocument = TestDocument.builder()
                .key(KEY)
                .title(VALUE)
                .build();
        documentRepository.save(testDocument);

        assertThat(documentRepository.findOne(KEY)).isEqualTo(testDocument);
    }

    @Ignore("Figure out how to rewrite ports for old client")
    @Test
    public void oldClientShouldWork() throws Exception {
        couchbaseClient.set(KEY, VALUE).get(5, TimeUnit.SECONDS);

        assertThat(couchbaseClient.get(KEY)).isEqualTo(VALUE);
    }

    @Test
    public void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.couchbase.bootstrapHttpDirectPort")).isNotEmpty();
        assertThat(environment.getProperty("embedded.couchbase.bootstrapCarrierDirectPort")).isNotEmpty();
        assertThat(environment.getProperty("embedded.couchbase.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.couchbase.bucket")).isNotEmpty();
        assertThat(environment.getProperty("embedded.couchbase.user")).isNotEmpty();
        assertThat(environment.getProperty("embedded.couchbase.password")).isNotEmpty();
        assertThat(System.getProperty("com.couchbase.bootstrapHttpDirectPort")).isNotEmpty();
        assertThat(System.getProperty("com.couchbase.bootstrapCarrierDirectPort")).isNotEmpty();
    }

    @Configuration
    @Import({SpringDataCouchbaseConfiguration.class, LegacyClientConfiguration.class})
    static class SpringBootApp {
    }

}
