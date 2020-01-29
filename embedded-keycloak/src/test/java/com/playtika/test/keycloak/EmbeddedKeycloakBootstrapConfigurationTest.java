/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Playtika
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
package com.playtika.test.keycloak;

import static com.playtika.test.keycloak.KeycloakProperties.BEAN_NAME_EMBEDDED_KEYCLOAK;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = KeycloakTestApplication.class
)
@ActiveProfiles("enabled")
public class EmbeddedKeycloakBootstrapConfigurationTest {

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    private KeycloakContainer keycloakContainer;

    @Test
    public void keycloakContainerAvailable() {
        assertThat(keycloakContainer).isNotNull();
    }

    @Test
    public void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.keycloak.auth-server-url"))
            .isEqualTo(format("http://%s:%d/auth", keycloakContainer.getContainerIpAddress(),
                keycloakContainer.getHttpPort()));

        assertThat(environment.getProperty("embedded.keycloak.host"))
            .isEqualTo(keycloakContainer.getIp());

        assertThat(environment.getProperty("embedded.keycloak.port.http", Integer.class))
            .isEqualTo(keycloakContainer.getHttpPort());
    }

    @Test
    public void shouldSetupDependsOnForKeycloakSecurityContext() {
        String[] beanNamesForType = beanFactory.getBeanNamesForType(
            KeycloakSpringBootConfigResolver.class);

        assertThat(beanNamesForType)
            .as("KeycloakSpringBootConfigResolver should be present")
            .hasSize(1);

        asList(beanNamesForType).forEach(this::hasDependsOn);
    }

    private void hasDependsOn(String beanName) {
        assertThat(beanFactory.getBeanDefinition(beanName).getDependsOn())
            .isNotNull()
            .isNotEmpty()
            .contains(BEAN_NAME_EMBEDDED_KEYCLOAK);
    }

    // TODO test keycloak connection
}
