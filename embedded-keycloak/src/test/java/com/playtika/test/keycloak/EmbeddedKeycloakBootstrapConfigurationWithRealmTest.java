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

import static org.assertj.core.api.Assertions.assertThat;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = KeycloakTestApplication.class
)
@ActiveProfiles("realm")
public class EmbeddedKeycloakBootstrapConfigurationWithRealmTest {

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    private KeycloakContainer keycloakContainer;

    @Test
    public void shouldGetTestRealmInfoFromKeycloak() {
        RestTemplate restTemplate = new RestTemplate();
        String baseUrl = environment.getProperty("embedded.keycloak.auth-server-url");

        RealmInfo realmInfo = restTemplate.getForObject(
            baseUrl + "/realms/test-realm",
            RealmInfo.class
        );

        assertThat(realmInfo.getRealm()).isEqualTo("test-realm");
    }

    @Test
    public void shouldGetAccessTokenFromKeycloak() {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String clientId = environment.getProperty("keycloak.resource");
        String clientSecret = environment.getProperty("keycloak.credentials.secret");

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("grant_type", "password");
        map.add("username", "keycloak-test");
        map.add("password", "123Start!");

        String baseUrl = environment.getProperty("embedded.keycloak.auth-server-url");

        KeyCloakToken token = restTemplate
            .postForObject(baseUrl + "/realms/test-realm/protocol/openid-connect/token",
                new HttpEntity<>(map, headers), KeyCloakToken.class);

        assertThat(token.getAccessToken()).isNotEmpty();
    }
}
