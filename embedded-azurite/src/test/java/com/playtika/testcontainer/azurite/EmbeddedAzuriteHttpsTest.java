package com.playtika.testcontainer.azurite;

import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.azure.AzuriteContainer;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = EmbeddedAzuriteHttpsTest.AzuriteHttpsTestConfiguration.class,
        properties = {"embedded.azurite.https-enabled=true"})
@Disabled("Create private and public key to check https")
class EmbeddedAzuriteHttpsTest {

    @Autowired
    AzuriteContainer azuriteContainer;

    @Autowired
    BlobServiceClient blobServiceClient;

    @Value("${embedded.azurite.blob-endpoint}")
    String blobEndpoint;

    @Test
    void blobEndpointUsesHttps() {
        assertThat(blobEndpoint).startsWith("https://");
    }

    @Test
    void connectionStringUsesHttps() {
        assertThat(azuriteContainer.getConnectionString()).contains("DefaultEndpointsProtocol=https");
    }

    @Test
    @DisplayName("basic blob operations work over HTTPS with the embedded self-signed certificate")
    void createAndDeleteContainerBlobOverHttps() {
        long containersBefore = blobServiceClient.listBlobContainers().stream().count();
        BlobContainerClient container = blobServiceClient.createBlobContainer(UUID.randomUUID().toString());
        assertThat(container.listBlobs().stream()).isEmpty();
        assertThat(blobServiceClient.listBlobContainers().stream().count()).isEqualTo(containersBefore + 1);
        container.delete();
        assertThat(blobServiceClient.listBlobContainers().stream().count()).isEqualTo(containersBefore);
    }

    @EnableAutoConfiguration
    @Configuration
    static class AzuriteHttpsTestConfiguration {

        @Bean
        BlobServiceClient blobServiceClient(AzuriteContainer azuriteContainer) throws SSLException {
            // Trust all certs so the embedded self-signed certificate is accepted in tests.
            io.netty.handler.ssl.SslContext insecureSslContext = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
            HttpClient reactor = HttpClient.create().secure(spec -> spec.sslContext(insecureSslContext));
            com.azure.core.http.HttpClient azureHttpClient = new NettyAsyncHttpClientBuilder(reactor).build();

            return new BlobServiceClientBuilder()
                    .connectionString(azuriteContainer.getConnectionString())
                    .httpClient(azureHttpClient)
                    .buildClient();
        }
    }
}
