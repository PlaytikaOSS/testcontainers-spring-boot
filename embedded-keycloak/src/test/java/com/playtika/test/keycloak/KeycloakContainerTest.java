package com.playtika.test.keycloak;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.playtika.test.keycloak.KeycloakContainer.ImportFileNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = KeycloakTestApplication.class,
    properties = "embedded.keycloak.import-file=non-existent.json"
)
@ActiveProfiles("disabled")
public class KeycloakContainerTest {

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private KeycloakProperties keycloakProperties;

    @Test
    public void shouldThrownNonExistentRealmFileException() {
        try (KeycloakContainer container = new KeycloakContainer(keycloakProperties,
            resourceLoader)) {
            assertThatExceptionOfType(ImportFileNotFoundException.class)
                .isThrownBy(container::configure);
        }
    }
}
