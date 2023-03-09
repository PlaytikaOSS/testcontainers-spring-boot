package com.playtika.test.azurite;

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
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = EmbeddedAzuriteBoostrapConfigurationTest.AzuriteTestConfiguration.class)
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

    @EnableAutoConfiguration
    public static class AzuriteTestConfiguration {

    }

}
