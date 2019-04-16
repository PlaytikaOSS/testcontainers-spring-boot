/*
* The MIT License (MIT)
*
* Copyright (c) 2018 Playtika
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
package com.playtika.test.voltdb;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.sql.CallableStatement;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = EmbeddedVoltDBBootstrapConfigurationTest.TestConfiguration.class)
@ActiveProfiles("enabled")
public class EmbeddedVoltDBBootstrapConfigurationTest {

    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ConfigurableEnvironment environment;

    @Test
    public void shouldConnect() {
        Map<String, Object> result = jdbcTemplate.call(con -> {
            CallableStatement call = con.prepareCall("{call @SystemInformation(?)}");
            call.setString(1, "overview");
            return call;
        }, Collections.emptyList());
        assertThat(result).isNotEmpty();
    }

    @Test
    public void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.voltdb.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.voltdb.host")).isNotEmpty();
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}
