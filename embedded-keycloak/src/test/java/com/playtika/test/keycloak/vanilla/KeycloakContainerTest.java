package com.playtika.test.keycloak.vanilla;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.playtika.test.keycloak.KeycloakContainer;
import com.playtika.test.keycloak.KeycloakContainer.ImportFileNotFoundException;
import com.playtika.test.keycloak.KeycloakProperties;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.ContainerLaunchException;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = VanillaTestApplication.class,
    properties = "embedded.keycloak.import-file=non-existent.json"
)
@ActiveProfiles("disabled")
public class KeycloakContainerTest {

    @Autowired
    private ResourceLoader resourceLoader;

    @Test
    public void shouldThrowImportFileNotFoundException() {
        KeycloakProperties properties = new KeycloakProperties();
        properties.setImportFile("non-existent.json");

        try (KeycloakContainer container = new KeycloakContainer(properties,
            resourceLoader)) {
            assertThatExceptionOfType(ContainerLaunchException.class)
                .isThrownBy(container::start)
                .withCauseInstanceOf(ImportFileNotFoundException.class);
        }
    }
}
