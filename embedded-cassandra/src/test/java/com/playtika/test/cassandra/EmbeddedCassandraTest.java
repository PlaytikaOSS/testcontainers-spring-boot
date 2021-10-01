package com.playtika.test.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import org.springframework.data.cassandra.repository.AllowFiltering;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static com.playtika.test.cassandra.CassandraProperties.BEAN_NAME_EMBEDDED_CASSANDRA;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = EmbeddedCassandraTest.TestConfiguration.class,
        properties = { "embedded.cassandra.enabled=true", "embedded.cassandra.waitTimeoutInSeconds=90" }
)
@ActiveProfiles("enabled")
public class EmbeddedCassandraTest {

    @Autowired
    protected ConfigurableListableBeanFactory beanFactory;

    @Autowired
    protected DummyCassandraRepository dummyCassandraRepository;

    @Autowired
    protected ConfigurableEnvironment environment;


    @Test
    public void springDataCassandraShouldWork() {
        DummyCassandraEntity entityToSave = new DummyCassandraEntity(UUID.randomUUID(), "name");
        dummyCassandraRepository.save(entityToSave);
        DummyCassandraEntity retrievedEntity = dummyCassandraRepository.findByName("name");
        assertThat(retrievedEntity).isEqualTo(entityToSave);
    }

    @Test
    public void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.cassandra.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.cassandra.host")).isNotEmpty();
    }

    @Test
    public void shouldSetupDependsOnForCqlSession() {
        assertDependsOnEmbeddedCassandra(CqlSession.class, "cassandraSession");
    }

    private void assertDependsOnEmbeddedCassandra(Class beanClass, String beanName) {
        String[] beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, beanClass);
        assertThat(beanNamesForType)
                .as(beanName + " should be present")
                .hasSize(1)
                .contains(beanName);
        asList(beanNamesForType).forEach(this::hasDependsOnEmbeddedCassandra);
    }

    private void hasDependsOnEmbeddedCassandra(String beanName) {
        assertThat(beanFactory.getBeanDefinition(beanName).getDependsOn())
                .isNotNull()
                .isNotEmpty()
                .contains(BEAN_NAME_EMBEDDED_CASSANDRA);
    }

    @EnableAutoConfiguration
    @Configuration
    @EnableCassandraRepositories(considerNestedRepositories = true)
    static class TestConfiguration { }

    @Data
    @Table
    @NoArgsConstructor
    @AllArgsConstructor
    static class DummyCassandraEntity {
        @PrimaryKey
        private UUID id;
        private String name;
    }

    interface DummyCassandraRepository extends CassandraRepository<DummyCassandraEntity, UUID> {
        @AllowFiltering
        DummyCassandraEntity findByName(String name);
    }
}
