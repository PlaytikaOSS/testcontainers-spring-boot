package com.playtika.testcontainer.postgresql;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.playtika.testcontainer.postgresql.FakeScheduledContainerBootstrapConfiguration.PROPERTY_VALUE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every {@code EmbeddedXxxBootstrapConfiguration} runs in Spring Cloud's bootstrap phase, a
 * separate context that fully completes - including flushing any scheduled container start and
 * registering its connection properties - before the main application context (where beans like
 * Spring Boot's {@code DataSourceProperties} bind {@code spring.datasource.*} placeholders) is
 * even built. This proves that guarantee still holds under
 * {@code embedded.containers.parallelStartup=true}, using a fake bootstrap bean
 * ({@link FakeScheduledContainerBootstrapConfiguration}) that schedules a property registration
 * instead of starting a real container. Note this still needs Docker to run:
 * {@code DockerPresenceBootstrapConfiguration}'s presence check is unconditional in the bootstrap
 * phase whenever {@code embedded.containers.enabled} isn't false - only real container startup is
 * avoided here. {@code TestConfiguration} is intentionally a plain, autoconfiguration-free
 * {@code @Configuration} (not the shared {@code dummyapp.TestApplication}) so no
 * {@code DataSourceAutoConfiguration} gets pulled in - this test never activates the "enabled"
 * profile, so {@code spring.datasource.url} is never set.
 */
@SpringBootTest(
        classes = ParallelStartupPropertyOrderingTest.TestConfiguration.class,
        properties = {
                "embedded.postgresql.enabled=false",
                "embedded.containers.parallelStartup=true"
        }
)
class ParallelStartupPropertyOrderingTest {

    @Autowired
    private PropertyHolder propertyHolder;

    @Test
    void scheduledPropertyIsAvailableBeforeMainContextBeanCreation() {
        assertThat(propertyHolder.value()).isEqualTo(PROPERTY_VALUE);
    }

    @Configuration
    static class TestConfiguration {

        @Bean
        PropertyHolder propertyHolder(@Value("${" + FakeScheduledContainerBootstrapConfiguration.PROPERTY_NAME + "}") String value) {
            return new PropertyHolder(value);
        }
    }

    record PropertyHolder(String value) {
    }
}
