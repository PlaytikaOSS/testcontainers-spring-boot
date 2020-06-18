/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Playtika
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
package com.playtika.test.vault;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.support.Versioned;

@SpringBootTest(properties = {
        "embedded.vault.secrets.secret_one=password1"
}
,classes = EmbeddedVaultBootstrapConfigurationTest.TestConfiguration.class)
public class EmbeddedVaultBootstrapConfigurationTest {

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    private VaultOperations vaultOperations;

    @Test
    public void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.vault.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.vault.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.vault.token")).isNotEmpty();
        assertThat(environment.getProperty("secret_one")).isNotEmpty();
    }

    @Test
    public void shouldReadASecret() {
        Versioned<Map<String, Object>> secrets = vaultOperations.opsForVersionedKeyValue("secret").get("application");

        assertThat(secrets.getData())
                .as("check secret")
                .containsExactly(entry("secret_one", "password1"));
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}
