package com.playtika.testcontainer.neo4j;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.types.Node;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;
import java.util.concurrent.Callable;

import static com.playtika.testcontainer.neo4j.Neo4jProperties.BEAN_NAME_EMBEDDED_NEO4J;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(
        classes = EmbeddedNeo4JBootstrapConfigurationTest.TestConfiguration.class
)
@ActiveProfiles("enabled")
public class EmbeddedNeo4JBootstrapConfigurationTest {

    @Autowired
    Driver driver;

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
    void springDataNeo4jShouldWork() {
        long friendId;

        try (Session session = driver.session()) {
            Record record = session.run("CREATE (n:Person{name:'Freddie'}),"
                    + " (n)-[:TEAMMATE{since: 1995}]->(:Person{name:'Frank'})"
                    + "RETURN n").single();

            Node friendNode = record.get("n").asNode();
            friendId = friendNode.id();
        }

        Person person = personRepository.findById(friendId).get();

        Set<TeamMateRelationship> loadedRelationship = person.getTeammates();
        assertThat(loadedRelationship).allSatisfy(relationship -> {
            assertThat(relationship.getSince()).isEqualTo(1995);
            assertThat(relationship.getTeamMate().getName()).isEqualTo("Frank");
        });
    }

    @Test
    public void shouldSetupDependsOnForAllClients() throws Exception {
        String[] beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, Driver.class);
        assertThat(beanNamesForType)
                .as("neo4jDriver should be present")
                .hasSize(1)
                .contains("neo4jDriver");
        asList(beanNamesForType).forEach(this::hasDependsOn);
    }

    private void hasDependsOn(String beanName) {
        assertThat(beanFactory.getBeanDefinition(beanName).getDependsOn())
                .isNotNull()
                .isNotEmpty()
                .contains(BEAN_NAME_EMBEDDED_NEO4J);
    }

    private static long durationOf(Callable<?> op) throws Exception {
        long start = System.currentTimeMillis();
        op.call();
        return System.currentTimeMillis() - start;
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
