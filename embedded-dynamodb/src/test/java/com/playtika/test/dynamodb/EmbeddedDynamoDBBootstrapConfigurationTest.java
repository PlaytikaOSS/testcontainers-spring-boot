package com.playtika.test.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import com.playtika.test.dynamodb.springdata.DynamoDBConfig;
import com.playtika.test.dynamodb.springdata.User;
import com.playtika.test.dynamodb.springdata.UserRepository;
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

import java.util.List;

import static com.playtika.test.dynamodb.DynamoDBProperties.BEAN_NAME_EMBEDDED_DYNAMODB;
import static java.util.Arrays.asList;
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
    private UserRepository repository;

    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @Test
    public void sampleTestCase() {
        User gosling = new User("James", "Gosling");
        repository.save(gosling);

        User hoeller = new User("Juergen", "Hoeller");
        repository.save(hoeller);

        List<User> result = repository.findByLastName("Gosling");
        Assertions.assertThat(result.size()).isOne();
        Assertions.assertThat(result).contains(gosling);
        log.info("Found in table: {}", result.get(0));
    }

    @Test
    public void shouldSetupDependsOnForAllDataAmazonDBs() throws Exception {
        String[] beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, AmazonDynamoDB.class);
        assertThat(beanNamesForType)
                .as("Auto-configured AmazonDynamoDB should be present")
                .hasSize(1)
                .contains("amazonDynamoDB");
        asList(beanNamesForType).forEach(this::hasDependsOn);
    }

    private void hasDependsOn(String beanName) {
        assertThat(beanFactory.getBeanDefinition(beanName).getDependsOn())
                .isNotNull()
                .isNotEmpty()
                .contains(BEAN_NAME_EMBEDDED_DYNAMODB);
    }


    @Slf4j
    @EnableAutoConfiguration
    @Configuration
    @Import(DynamoDBConfig.class)
    static class TestConfiguration implements InitializingBean {

        @Autowired
        private AmazonDynamoDB amazonDynamoDB;

        @Autowired
        private DynamoDBMapper mapper;

        @Override
        public void afterPropertiesSet() throws Exception {
            setupTable();
        }

        void setupTable() throws InterruptedException {

            CreateTableRequest ctr = mapper.generateCreateTableRequest(User.class)
                    .withProvisionedThroughput(new ProvisionedThroughput(1L, 1L));

            boolean tableWasCreatedForTest = TableUtils.createTableIfNotExists(amazonDynamoDB, ctr);

            if (tableWasCreatedForTest) {
                log.info("Created table {}", ctr.getTableName());
            }

            TableUtils.waitUntilActive(amazonDynamoDB, ctr.getTableName());

            log.info("Table {} is active", ctr.getTableName());
        }
    }
}
