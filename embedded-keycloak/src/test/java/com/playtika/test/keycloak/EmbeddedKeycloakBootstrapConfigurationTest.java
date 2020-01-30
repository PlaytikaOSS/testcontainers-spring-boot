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

import static com.playtika.test.keycloak.KeycloakProperties.DEFAULT_REALM;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = KeycloakTestApplication.class
)
@ActiveProfiles("enabled")
public class EmbeddedKeycloakBootstrapConfigurationTest {

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    private KeycloakContainer keycloakContainer;

    @Test
    public void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.keycloak.auth-server-url"))
            .isEqualTo(format("http://%s:%d/auth", keycloakContainer.getContainerIpAddress(),
                keycloakContainer.getHttpPort()));

        assertThat(environment.getProperty("embedded.keycloak.host"))
            .isEqualTo(keycloakContainer.getIp());

        assertThat(environment.getProperty("embedded.keycloak.http-port", Integer.class))
            .isEqualTo(keycloakContainer.getHttpPort());
    }

    @Test
    public void shouldGetMasterRealmInfoFromKeycloak() {
        RestTemplate restTemplate = new RestTemplate();

        String url = environment.getProperty("embedded.keycloak.auth-server-url");

        RealmInfo realmInfo = restTemplate.getForObject(
            url + "/realms/" + DEFAULT_REALM,
            RealmInfo.class);

        assertThat(realmInfo.getRealm()).isEqualTo(DEFAULT_REALM);
    }
}
