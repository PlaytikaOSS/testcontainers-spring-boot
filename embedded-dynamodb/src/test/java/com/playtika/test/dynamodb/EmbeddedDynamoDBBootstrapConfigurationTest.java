/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Playtika
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.PostConstruct;
import java.util.List;

import static com.playtika.test.dynamodb.DynamoDBProperties.BEAN_NAME_EMBEDDED_DYNAMODB;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

@Slf4j
@RunWith(SpringRunner.class)
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
        Assert.assertThat(result.size(), is(1));
        Assert.assertThat(result, hasItem(gosling));
        log.info("Found in table: {}", result.get(0));
    }

    @Test
    public void shouldSetupDependsOnForAllDataAmazonDBs() throws Exception {
        String[] beanNamesForType = beanFactory.getBeanNamesForType(AmazonDynamoDB.class);
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
    static class TestConfiguration {

        @Autowired
        private AmazonDynamoDB amazonDynamoDB;

        @Autowired
        private DynamoDBMapper mapper;

        @PostConstruct
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