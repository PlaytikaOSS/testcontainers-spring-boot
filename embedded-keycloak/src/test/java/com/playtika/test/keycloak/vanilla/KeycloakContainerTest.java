package com.playtika.test.keycloak.vanilla;

import com.playtika.test.keycloak.KeycloakContainer;
import com.playtika.test.keycloak.KeycloakContainer.ImportFileNotFoundException;
import com.playtika.test.keycloak.KeycloakProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.testcontainers.containers.ContainerLaunchException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class KeycloakContainerTest {

    @Mock
    ResourceLoader resourceLoader;

    @Spy
    KeycloakProperties properties = setupProperties();

    @InjectMocks
    KeycloakContainer container;


    @Test
    public void shouldThrowImportFileNotFoundExceptionWhenImportFileDoesNotExist() {
        Resource importFile = mock(Resource.class);
        when(importFile.exists()).thenReturn(false);
        when(resourceLoader.getResource(any())).thenReturn(importFile);

        assertThatThrownBy(container::start)
                .isInstanceOf(ContainerLaunchException.class)
                .hasCauseInstanceOf(ImportFileNotFoundException.class);
    }

    private KeycloakProperties setupProperties() {
        KeycloakProperties properties = new KeycloakProperties();
        properties.setImportFile("any-import-file.json");
        return properties;
    }
}
