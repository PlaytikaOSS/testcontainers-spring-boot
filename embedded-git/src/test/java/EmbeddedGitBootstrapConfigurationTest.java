import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(
        classes = EmbeddedGitBootstrapConfigurationTest.TestConfiguration.class,
        properties = {
                "spring.profiles.active=enabled",
                "embedded.git.enabled=true",
                "embedded.git.path-to-repositories=src/test"
        }
)
class EmbeddedGitBootstrapConfigurationTest {
    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @Autowired
    ConfigurableEnvironment environment;

    @Test
    public void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.git.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.git.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.git.password")).isEqualTo("embedded-git-password");
        assertThat(environment.getProperty("embedded.git.path-to-repositories")).isEqualTo("src/test");
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}
