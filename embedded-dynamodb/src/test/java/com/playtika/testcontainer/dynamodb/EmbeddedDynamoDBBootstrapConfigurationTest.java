package com.playtika.testcontainer.dynamodb;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(
        classes = EmbeddedDynamoDBBootstrapConfigurationTest.TestConfiguration.class,
        properties = {
                "embedded.dynamodb.enabled=true",
                "amazon.aws.accesskey=n/a",
                "amazon.aws.secretkey=n/a",
                "amazon.aws.endpoint=http://${embedded.dynamodb.host}:${embedded.dynamodb.port}",
                "amazon.aws.signingRegion=us-east-1"
        })
@ActiveProfiles("enabled")
public class EmbeddedDynamoDBBootstrapConfigurationTest {

    @Autowired
    private AmazonDynamoDB amazonDynamoDB;

    @Test
    public void sampleTestCase() throws InterruptedException {
        String tableName = "User";

        // Create table first
        CreateTableRequest createTableRequest = new CreateTableRequest()
                .withTableName(tableName)
                .withKeySchema(
                        new KeySchemaElement("firstName", KeyType.HASH),
                        new KeySchemaElement("lastName", KeyType.RANGE))
                .withAttributeDefinitions(
                        new AttributeDefinition("firstName", ScalarAttributeType.S),
                        new AttributeDefinition("lastName", ScalarAttributeType.S))
                .withProvisionedThroughput(new ProvisionedThroughput(1L, 1L));

        boolean tableWasCreatedForTest = TableUtils.createTableIfNotExists(amazonDynamoDB, createTableRequest);

        if (tableWasCreatedForTest) {
            log.info("Created table {}", tableName);
        }

        TableUtils.waitUntilActive(amazonDynamoDB, tableName);
        log.info("Table {} is active", tableName);

        // Put an item
        String firstName = "James";
        String lastName = "Gosling";
        Map<String, com.amazonaws.services.dynamodbv2.model.AttributeValue> item = new HashMap<>();
        item.put("firstName", new com.amazonaws.services.dynamodbv2.model.AttributeValue().withS(firstName));
        item.put("lastName", new com.amazonaws.services.dynamodbv2.model.AttributeValue().withS(lastName));

        PutItemRequest putItemRequest = new PutItemRequest()
                .withTableName(tableName)
                .withItem(item);
        amazonDynamoDB.putItem(putItemRequest);

        log.info("Successfully put item in table: {}", tableName);
        assertThat(amazonDynamoDB.describeTable(tableName).getTable().getTableStatus()).isEqualTo("ACTIVE");
    }

    @Slf4j
    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration implements InitializingBean {

        @Value("${amazon.aws.accesskey}")
        private String amazonAWSAccessKey;

        @Value("${amazon.aws.endpoint}")
        private String amazonAWSEndpoint;

        @Value("${amazon.aws.signingRegion:us-east-1}")
        private String amazonAWSSigningRegion;

        @Value("${amazon.aws.secretkey}")
        private String amazonAWSSecretKey;

        @Bean
        public AWSCredentialsProvider amazonAWSCredentialsProvider(AWSCredentials amazonAWSCredentials) {
            return new AWSStaticCredentialsProvider(amazonAWSCredentials);
        }

        @Bean
        public AWSCredentials amazonAWSCredentials() {
            return new BasicAWSCredentials(amazonAWSAccessKey, amazonAWSSecretKey);
        }

        @Bean
        public AmazonDynamoDB amazonDynamoDB(AWSCredentialsProvider amazonAWSCredentialsProvider) {
            AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(amazonAWSEndpoint, amazonAWSSigningRegion);
            return AmazonDynamoDBClientBuilder
                    .standard()
                    .withCredentials(amazonAWSCredentialsProvider)
                    .withEndpointConfiguration(endpointConfiguration)
                    .build();
        }

        @Override
        public void afterPropertiesSet() throws Exception {
            // Table setup will happen after bean creation
        }
    }
}
