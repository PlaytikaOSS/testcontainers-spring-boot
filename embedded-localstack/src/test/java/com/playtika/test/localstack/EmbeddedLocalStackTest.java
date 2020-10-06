package com.playtika.test.localstack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = EmbeddedLocalStackTest.TestConfiguration.class,
        properties = "embedded.localstack.services=S3,SQS")
public class EmbeddedLocalStackTest {
    @Value("${embedded.localstack.accessKey}")
    private String accessKey;

    @Value("${embedded.localstack.secretKey}")
    private String secretKey;

    @Value("${embedded.localstack.S3}")
    private String s3Endpoint;

    @Value("${embedded.localstack.SQS}")
    private String sqsEndpoint;

    @Value("${embedded.localstack.region}")
    private String region;

    @Value("${embedded.localstack.SQS.port}")
    private String sqsPort;

    @Autowired
    private ConfigurableEnvironment environment;

    @Test
    public void shouldStartS3() throws IOException {
        AmazonS3 s3 = AmazonS3ClientBuilder
                .standard()
                .withEndpointConfiguration(getEndpointConfiguration(s3Endpoint))
                .withCredentials(getAwsCredentialsProvider())
                .build();

        final String bucketName = "foo";
        s3.createBucket(bucketName);
        s3.putObject(bucketName, "bar", "baz");

        final List<Bucket> buckets = s3.listBuckets();
        final Optional<Bucket> maybeBucket = buckets.stream().filter(b -> b.getName().equals(bucketName)).findFirst();
        assertThat(maybeBucket).isPresent();

        final Bucket bucket = maybeBucket.get();
        assertThat(bucketName).isEqualTo(bucket.getName());

        final ObjectListing objectListing = s3.listObjects(bucketName);
        assertThat(objectListing.getObjectSummaries()).hasSize(1);
        final S3Object object = s3.getObject(bucketName, "bar");
        final String content = IOUtils.toString(object.getObjectContent(), StandardCharsets.UTF_8);
        assertThat(content).isEqualTo("baz");
    }

    @Test
    public void shouldStartSQS() {
        AmazonSQS sqs = AmazonSQSClientBuilder.standard()
                                              .withEndpointConfiguration(getEndpointConfiguration(sqsEndpoint))
                                              .withCredentials(getAwsCredentialsProvider())
                                              .build();

        CreateQueueResult queueResult = sqs.createQueue("baz");
        String fooQueueUrl = queueResult.getQueueUrl();
        assertThat(fooQueueUrl).
                                       contains("http://" + DockerClientFactory.instance().dockerHostIpAddress() + ":" + sqsPort);

        sqs.sendMessage(fooQueueUrl, "test");
        final long messageCount = sqs.receiveMessage(fooQueueUrl).getMessages().stream()
                                     .filter(message -> message.getBody().equals("test"))
                                     .count();
        assertThat(messageCount).isEqualTo(1);
    }

    @Test
    public void shouldProduceLocalstackProperties() {
        assertThat(environment.getProperty("embedded.localstack.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.localstack.accessKey")).isNotEmpty();
        assertThat(environment.getProperty("embedded.localstack.secretKey")).isNotEmpty();
        assertThat(environment.getProperty("embedded.localstack.region")).isNotEmpty();
        assertThat(environment.getProperty("embedded.localstack.S3")).isNotEmpty();
        assertThat(environment.getProperty("embedded.localstack.S3.port")).isNotEmpty();
    }

    private AwsClientBuilder.EndpointConfiguration getEndpointConfiguration(String endpoint) {
        return new AwsClientBuilder.EndpointConfiguration(endpoint, region);
    }

    private AWSCredentialsProvider getAwsCredentialsProvider() {
        return new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));
    }

    @Configuration
    @EnableAutoConfiguration
    static class TestConfiguration {
    }
}
