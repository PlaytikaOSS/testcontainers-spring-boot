package com.playtika.testcontainer.storage;

import com.google.cloud.NoCredentials;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.playtika.testcontainer.storage.EmbeddedStorageBootstrapConfigurationTest.TestConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;
import java.nio.ByteBuffer;

import static com.playtika.testcontainer.storage.StorageProperties.BEAN_NAME_EMBEDDED_GOOGLE_STORAGE_SERVER;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(classes = EmbeddedStorageBootstrapConfigurationTest.TestConfig.class)
@Import(TestConfig.class)
@ActiveProfiles("enabled")
public class EmbeddedStorageBootstrapConfigurationTest {

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    private Storage storage;

    @EnableAutoConfiguration
    @Configuration
    static class TestConfig {

        @Bean
        Storage storage(
            @Value("${my-test-project.storage-host}") String storageHost,
            @Value("${spring.cloud.gcp.project-id}") String projectId) {
            return StorageOptions.newBuilder()
                .setHost(storageHost)
                .setProjectId(projectId)
                .setCredentials(NoCredentials.getInstance())
                .build()
                .getService();
        }
    }

    @Test
    public void propertiesAreAvailable() {
        // host could be allocated dynamically
        assertThat(environment.getProperty("embedded.google.storage.host")).isNotEmpty();
        // port is assigned dynamically
        assertThat(environment.getProperty("embedded.google.storage.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.google.storage.endpoint")).isNotEmpty();
        assertThat(environment.getProperty("embedded.google.storage.project-id")).isEqualTo("my-project-id-enabled");
        assertThat(environment.getProperty("embedded.google.storage.bucket-location")).isEqualTo("US-EAST1");
    }

    @Test
    public void bucketsPredefinedAreAvailable() {
        Bucket predefinedBucket0 = storage.get("bucket0");
        Bucket predefinedBucket1 = storage.get("bucket1");

        assertThat(predefinedBucket0.exists()).isTrue();
        assertThat(predefinedBucket1.exists()).isTrue();
    }

    @Test
    public void shouldUploadAndDownloadFile() {
        storage.create(BlobInfo.newBuilder("bucket1", "test_file.txt").build(), "test_file_content".getBytes());

        Blob testFile = storage.get("bucket1", "test_file.txt");

        assertThat(testFile.getContent()).isEqualTo("test_file_content".getBytes());
    }


    @Test
    public void shouldChannelingUploadAndDownloadFile() throws IOException {
        WriteChannel writeChannel = storage.writer(BlobInfo.newBuilder("bucket1", "test_file2.txt").build());
        writeChannel.write(ByteBuffer.wrap("line1\n".getBytes()));
        writeChannel.write(ByteBuffer.wrap("line2\n".getBytes()));
        writeChannel.close();

        Blob testFile = storage.get("bucket1", "test_file2.txt");

        assertThat(testFile.getContent()).isEqualTo("line1\nline2\n".getBytes());
    }

    @Test
    public void shouldSetupDependsOnForStorage() {
        String[] beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, Storage.class);
        assertThat(beanNamesForType)
            .as("storage should be present")
            .hasSize(1)
            .contains("storage");
        asList(beanNamesForType).forEach(this::hasDependsOn);
    }

    @Test
    public void shouldHaveContainerWithExpectedDefaultProperties() {
        assertThat(beanFactory.getBean(BEAN_NAME_EMBEDDED_GOOGLE_STORAGE_SERVER))
            .isNotNull()
            .isInstanceOf(GenericContainer.class)
            .satisfies(genericContainer -> {
                GenericContainer<?> container = (GenericContainer<?>) genericContainer;

                assertThat(container.getExposedPorts()).containsExactly(4443);
                assertThat(container.getContainerInfo().getConfig().getEntrypoint())
                    .containsExactly(
                        "/bin/fake-gcs-server",
                        "-backend", "memory",
                        "-scheme", "http",
                        "-host", "0.0.0.0",
                        "-port", "4443",
                        "-location", "US-EAST1");
            });
    }

    private void hasDependsOn(String beanName) {
        assertThat(beanFactory.getBeanDefinition(beanName).getDependsOn())
            .isNotNull()
            .isNotEmpty()
            .contains(BEAN_NAME_EMBEDDED_GOOGLE_STORAGE_SERVER);
    }
}
