package com.playtika.testcontainer.dynamodb;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;

import static com.playtika.testcontainer.dynamodb.DynamoDBProperties.BEAN_NAME_EMBEDDED_DYNAMODB;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = EmbeddedDynamoDBDependenciesTest.TestConfiguration.class,
        properties = {
                "embedded.dynamodb.enabled=true",
                "amazon.aws.accesskey=n/a",
                "amazon.aws.secretkey=n/a",
                "amazon.aws.endpoint=http://${embedded.dynamodb.host}:${embedded.dynamodb.port}",
                "amazon.aws.signingRegion=us-east-1"
        })
@ActiveProfiles("enabled")
public class EmbeddedDynamoDBDependenciesTest {

    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @Test
    public void shouldSetupDependsOnForAllDataAmazonDBs() throws Exception {
        String[] beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, AmazonDynamoDB.class);
        assertThat(beanNamesForType)
                .as("Auto-configured AmazonDynamoDB should be present")
                .hasSize(1)
                .contains("amazonDynamoDB");
        Arrays.asList(beanNamesForType).forEach(this::hasDependsOn);
    }

    private void hasDependsOn(String beanName) {
        assertThat(beanFactory.getBeanDefinition(beanName).getDependsOn())
                .isNotNull()
                .isNotEmpty()
                .contains(BEAN_NAME_EMBEDDED_DYNAMODB);
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {

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
    }
}
