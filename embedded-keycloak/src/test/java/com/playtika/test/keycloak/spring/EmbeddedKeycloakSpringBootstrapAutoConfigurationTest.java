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
package com.playtika.test.keycloak.spring;

import static com.playtika.test.keycloak.util.KeycloakClient.newKeycloakClient;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.GET;

import com.playtika.test.keycloak.util.KeyCloakToken;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = SpringTestApplication.class,
    webEnvironment = RANDOM_PORT)
@ActiveProfiles({"enabled", "realm"})
public class EmbeddedKeycloakSpringBootstrapAutoConfigurationTest {

    @Autowired
    private Environment environment;

    @LocalServerPort
    private int httpPort;

    @Test
    public void shouldRunThroughSpringSecurity() {
        assertThat(callSecuredPingEndpoint()).isEqualTo("pong");
    }

    private String callSecuredPingEndpoint() {
        KeyCloakToken keyCloakToken = newKeycloakClient(environment).keycloakToken();

        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION, format("Bearer %s", keyCloakToken.getAccessToken()));

        RestOperations restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:" + httpPort + "/api/echo",
            GET,
            new HttpEntity<>(headers),
            String.class
        );

        return response.getBody();
    }
}
