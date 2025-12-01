package com.playtika.testcontainer.azurite;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueServiceClient;
import com.azure.storage.queue.QueueServiceClientBuilder;
import com.azure.storage.queue.models.QueueMessageItem;
import com.azure.storage.queue.models.SendMessageResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = EmbeddedAzuriteBoostrapConfigurationTest.AzuriteTestConfiguration.class)
@ActiveProfiles("test")
class EmbeddedAzuriteBoostrapConfigurationTest {

    @Autowired
    BlobServiceClientBuilder blobServiceClientBuilder;

    @Autowired
    QueueServiceClientBuilder queueServiceClientBuilder;

    @Test
    void accountName() {
        BlobServiceClient blobServiceClient = blobServiceClientBuilder.buildClient();
        assertThat(blobServiceClient.getAccountName()).isEqualTo(AzuriteProperties.ACCOUNT_NAME);
    }

    @Test
    @DisplayName("do some basic operations with blob to show that azurite is running and working correctly")
    void createAndDeleteContainerBlob() {
        BlobServiceClient blobServiceClient = blobServiceClientBuilder.buildClient();
        long containersBefore = blobServiceClient.listBlobContainers().stream().count();
        BlobContainerClient container = blobServiceClient.createBlobContainer(UUID.randomUUID().toString());
        assertThat(container.listBlobs().stream()).isEmpty();
        assertThat(blobServiceClient.listBlobContainers().stream().count()).isEqualTo(containersBefore + 1);
        container.delete();
        assertThat(blobServiceClient.listBlobContainers().stream().count()).isEqualTo(containersBefore);
    }

    @Test
    @DisplayName("do some basic operations with queue to show that azurite is running and working correctly")
    void createAndDeleteContainerQueue() {
        QueueServiceClient queueServiceClient = queueServiceClientBuilder.buildClient();
        QueueClient queueClient = queueServiceClient.createQueue(UUID.randomUUID().toString());
        SendMessageResult sendMessageResult = queueClient.sendMessage("test");
        QueueMessageItem queueMessageItem = queueClient.receiveMessage();
        assertThat(queueMessageItem.getBody().toString()).isEqualTo("test");
        assertThat(queueMessageItem.getMessageId().toString()).isEqualTo(sendMessageResult.getMessageId());
        queueClient.delete();
    }

    @Configuration
    @EnableAutoConfiguration
    public static class AzuriteTestConfiguration {

        @Bean
        public BlobServiceClientBuilder blobServiceClientBuilder(
                @Value("${embedded.azurite.account-name}") String accountName,
                @Value("${embedded.azurite.account-key}") String accountKey,
                @Value("${embedded.azurite.blob-endpoint}") String endpoint) {
            return new BlobServiceClientBuilder()
                    .connectionString(String.format("DefaultEndpointsProtocol=http;AccountName=%s;AccountKey=%s;BlobEndpoint=%s;",
                            accountName, accountKey, endpoint));
        }

        @Bean
        public QueueServiceClientBuilder queueServiceClientBuilder(
                @Value("${embedded.azurite.account-name}") String accountName,
                @Value("${embedded.azurite.account-key}") String accountKey,
                @Value("${embedded.azurite.queue-endpoint}") String endpoint) {
            return new QueueServiceClientBuilder()
                    .connectionString(String.format("DefaultEndpointsProtocol=http;AccountName=%s;AccountKey=%s;QueueEndpoint=%s;",
                            accountName, accountKey, endpoint));
        }
    }

}
