package com.playtika.testcontainer.opensearch.springdata;

import com.playtika.testcontainer.opensearch.EmbeddedOpenSearchBootstrapConfigurationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(value = "credentials", inheritProfiles = false)
@SpringBootTest(
        classes = EmbeddedOpenSearchBootstrapConfigurationTest.Config.class,
        properties = {
                "embedded.opensearch.enabled=true",
                "embedded.opensearch.credentials-enabled=true",
                "embedded.opensearch.allow-insecure=true"
        })
public class SpringDataWithAuthTest extends SpringDataTest {

    @Test
    public void securityPropertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.opensearch.credentials-enabled"))
                .isNotEmpty()
                .isEqualTo("true");
        assertThat(environment.getProperty("embedded.opensearch.allow-insecure"))
                .isNotEmpty()
                .isEqualTo("true");
    }
}
