/*
* The MIT License (MIT)
*
* Copyright (c) 2017 Playtika
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
package com.playtika.test.neo4j;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static com.playtika.test.neo4j.Neo4jProperties.BEAN_NAME_EMBEDDED_NEO4J;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = EmbeddedNeo4JBootstrapConfigurationTest.TestConfiguration.class)
@ActiveProfiles("test")
public class EmbeddedNeo4JBootstrapConfigurationTest {

    @Autowired
    PersonRepository personRepository;

    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @Configuration
    @EnableAutoConfiguration
    @EnableNeo4jRepositories
    static class TestConfiguration {
    }

    @Autowired
    ConfigurableEnvironment environment;

    @Test
    public void springDataNeo4jShouldWork() throws Exception {
        personRepository.deleteAll();

        Person greg = new Person("Greg");
        Person roy = new Person("Roy");
        Person craig = new Person("Craig");

        List<Person> team = asList(greg, roy, craig);

        personRepository.save(greg);
        personRepository.save(roy);
        personRepository.save(craig);

        greg = personRepository.findByName(greg.getName());
        greg.worksWith(roy);
        greg.worksWith(craig);
        personRepository.save(greg);

        roy = personRepository.findByName(roy.getName());
        roy.worksWith(craig);
        // We already know that roy works with greg
        personRepository.save(roy);

        // We already know craig works with roy and greg

        log.info("Lookup each person by name...");
        team.forEach(person -> log.info(personRepository.findByName(person.getName()).toString()));
        team.forEach(person -> assertThat(personRepository.findByName(person.getName()).getTeammates().size()).isEqualTo(2));
    }

    @Test
    public void shouldSetupDependsOnForAllClients() throws Exception {
        String[] beanNamesForType = beanFactory.getBeanNamesForType(SessionFactory.class);
        assertThat(beanNamesForType)
                .as("sessionFactory should be present")
                .hasSize(1)
                .contains("sessionFactory");
        asList(beanNamesForType).forEach(this::hasDependsOn);
    }

    private void hasDependsOn(String beanName) {
        assertThat(beanFactory.getBeanDefinition(beanName).getDependsOn())
                .isNotNull()
                .isNotEmpty()
                .contains(BEAN_NAME_EMBEDDED_NEO4J);
    }

    @Test
    public void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.neo4j.httpsPort")).isNotEmpty();
        assertThat(environment.getProperty("embedded.neo4j.boltPort")).isNotEmpty();
        assertThat(environment.getProperty("embedded.neo4j.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.neo4j.user")).isNotEmpty();
        assertThat(environment.getProperty("embedded.neo4j.password")).isNotEmpty();
    }
}
