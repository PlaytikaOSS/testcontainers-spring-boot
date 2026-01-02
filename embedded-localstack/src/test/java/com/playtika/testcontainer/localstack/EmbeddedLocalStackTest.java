package com.playtika.testcontainer.localstack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = EmbeddedLocalStackTest.TestConfiguration.class,
        properties = {
                "embedded.localstack.services=S3,SQS"
        }
)
public class EmbeddedLocalStackTest {
    @Value("${embedded.localstack.accessKey}")
    private String accessKey;

    @Value("${embedded.localstack.secretAccessKey}")
    private String secretAccessKey;

    @Value("${embedded.localstack.S3}")
    private String s3Endpoint;

    @Value("${embedded.localstack.SQS}")
    private String sqsEndpoint;

    @Autowired
    private ConfigurableEnvironment environment;

    @ParameterizedTest(name = "{0}")
    @EnumSource(ClientCreationStrategy.class)
    public void shouldStartS3(ClientCreationStrategy strategy) {
        S3Client s3 = strategy.createS3Client(
            s3Endpoint,
            Region.US_WEST_2,
            accessKey,
            secretAccessKey);

        String bucketName = "foo-" + strategy.name().toLowerCase().replace("_", "-");
        s3.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());

        String objectKey = "bar";
        String objectContent = "baz";
        s3.putObject(PutObjectRequest.builder().bucket(bucketName).key(objectKey).build(), RequestBody.fromString(objectContent));

        List<Bucket> buckets = s3.listBuckets().buckets();
        Optional<Bucket> maybeBucket = buckets.stream().filter(b -> b.name().equals(bucketName)).findFirst();
        assertThat(maybeBucket).isPresent();

        Bucket bucket = maybeBucket.get();
        assertThat(bucketName).isEqualTo(bucket.name());

        ListObjectsResponse listObjectsResponse = s3.listObjects(ListObjectsRequest.builder().bucket(bucketName).build());
        assertThat(listObjectsResponse.contents()).hasSize(1);

        ResponseBytes<GetObjectResponse> object = s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucketName).key(objectKey).build());
        assertThat(object.asByteArray()).asString(StandardCharsets.UTF_8).isEqualTo(objectContent);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(ClientCreationStrategy.class)
    public void shouldStartSQS(ClientCreationStrategy strategy) {
        SqsClient sqs = strategy.createSqsClient(
            sqsEndpoint,
            Region.US_WEST_2,
            accessKey,
            secretAccessKey);

        String queueName = "baz-" + strategy.name().toLowerCase().replace("_", "-");
        CreateQueueResponse createQueueResponse = sqs.createQueue(CreateQueueRequest.builder().queueName(queueName).build());
        String queueUrl = createQueueResponse.queueUrl();

        String messageBody = "test";
        sqs.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageBody(messageBody).build());

        long messageCount = sqs.receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueUrl).build()).messages().stream()
            .filter(message -> message.body().equals(messageBody))
            .count();
        assertThat(messageCount).isEqualTo(1);
    }

    @Test
    public void shouldProduceLocalstackProperties() {
        assertThat(environment.getProperty("embedded.localstack.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.localstack.endpointUrl")).isNotEmpty();
        assertThat(environment.getProperty("embedded.localstack.accessKey")).isNotEmpty();
        assertThat(environment.getProperty("embedded.localstack.secretAccessKey")).isNotEmpty();
        assertThat(environment.getProperty("embedded.localstack.S3")).isNotEmpty();
        assertThat(environment.getProperty("embedded.localstack.S3.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.localstack.SQS")).isNotEmpty();
        assertThat(environment.getProperty("embedded.localstack.SQS.port")).isNotEmpty();
    }

    @Configuration
    @EnableAutoConfiguration
    static class TestConfiguration {
    }

    public enum ClientCreationStrategy {
        EXPLICIT {
            @Override
            public S3Client createS3Client(String endpoint, Region region,
                                           String accessKey, String secretAccessKey) {
                return S3Client.builder()
                    .region(region)
                    .endpointOverride(URI.create(endpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretAccessKey)))
                    .build();
            }

            @Override
            public SqsClient createSqsClient(String endpoint, Region region,
                                             String accessKey, String secretAccessKey) {
                return SqsClient.builder()
                    .region(region)
                    .endpointOverride(URI.create(endpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretAccessKey)))
                    .build();
            }
        },

        AUTO_DISCOVERY {
            @Override
            public S3Client createS3Client(String endpoint, Region region,
                                           String accessKey, String secretAccessKey) {
                // System properties are already set by EmbeddedLocalStackBootstrapConfiguration
                // Client relies on auto-discovery from those system properties
                return S3Client.builder()
                    .region(region)
                    .build();
            }

            @Override
            public SqsClient createSqsClient(String endpoint, Region region,
                                             String accessKey, String secretAccessKey) {
                // System properties are already set by EmbeddedLocalStackBootstrapConfiguration
                // Client relies on auto-discovery from those system properties
                return SqsClient.builder()
                    .region(region)
                    .build();
            }
        };

        public abstract S3Client createS3Client(String endpoint, Region region,
                                                String accessKey, String secretAccessKey);

        public abstract SqsClient createSqsClient(String endpoint, Region region,
                                                  String accessKey, String secretAccessKey);
    }
}
