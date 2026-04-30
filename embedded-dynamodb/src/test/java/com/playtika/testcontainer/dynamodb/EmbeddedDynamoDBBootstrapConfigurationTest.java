package com.playtika.testcontainer.dynamodb;

import com.playtika.testcontainer.dynamodb.springdata.User;
import io.awspring.cloud.autoconfigure.dynamodb.DynamoDbAutoConfiguration;
import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(
        classes = EmbeddedDynamoDBBootstrapConfigurationTest.TestConfiguration.class,
        properties = {
                "embedded.dynamodb.enabled=true",
                "spring.profiles.active=enabled"
        })
public class EmbeddedDynamoDBBootstrapConfigurationTest {

    @Autowired
    DynamoDbTemplate dynamoDbTemplate;

    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @Test
    public void sampleTestCase() {
        User gosling = new User("1", "James");
        dynamoDbTemplate.save(gosling);

        User hoeller = new User("2", "Juergen");
        dynamoDbTemplate.save(hoeller);

        QueryEnhancedRequest request =  QueryEnhancedRequest.builder().queryConditional(QueryConditional.keyEqualTo(Key.builder().partitionValue("1").build())).build();
        PageIterable<User> result = dynamoDbTemplate.query(request, User.class);
        Assertions.assertThat(result.items().stream().count()).isOne();
        Assertions.assertThat(result.items()).contains(gosling);
        log.info("Found in table: {}", result.stream().findFirst());
    }

    @Test
    public void shouldSetupDependsOnForAllDataAmazonDBs() throws Exception {
        String[] beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, DynamoDbClient.class);
        assertThat(beanNamesForType)
                .as("Auto-configured DynamoDbClient should be present")
                .hasSize(1)
                .contains("dynamoDbClient");
    }


    @Slf4j
    @EnableAutoConfiguration
    @Configuration
    @Import(DynamoDbAutoConfiguration.class)
    static class TestConfiguration implements InitializingBean {

        @Autowired
        DynamoDbClient dynamoDbClient;

        @Override
        public void afterPropertiesSet() throws Exception {
            setupTable();
        }

        void setupTable() {

            ProvisionedThroughput throughput =  ProvisionedThroughput.builder().readCapacityUnits(5L).writeCapacityUnits(5L).build();

           final KeySchemaElement[] keySchemes = {KeySchemaElement.builder().keyType(KeyType.HASH).attributeName("id").build(),
                    KeySchemaElement.builder().keyType(KeyType.RANGE).attributeName("firstName").build()};

            AttributeDefinition[] attributeDefinitions = {AttributeDefinition.builder().attributeName("id").attributeType(ScalarAttributeType.S).build(),
                    AttributeDefinition.builder().attributeName("firstName").attributeType(ScalarAttributeType.S).build()};

            CreateTableRequest ctr = CreateTableRequest.builder().tableName("user")
                    .keySchema(keySchemes)
                    .attributeDefinitions(attributeDefinitions)
                    .provisionedThroughput(throughput).build();

            CreateTableResponse response = dynamoDbClient.createTable(ctr);
            TableDescription tableDescription = response.tableDescription();
            TableStatus tableStatus = tableDescription.tableStatus();
            if (tableStatus == TableStatus.CREATING) {
                log.info("Creating table {}", tableDescription.tableName());
            }else if (tableStatus == TableStatus.ACTIVE) {
                log.info("Table {} is active", tableDescription.tableName());
            }
        }
    }
}
