/*
* The MIT License (MIT)
*
* Copyright (c) 2017 Playtika
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
package com.playtika.test.memsql;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = EmbeddedMemSqlAutoConfigurationTest.TestConfiguration.class)
public class EmbeddedMemSqlAutoConfigurationTest {

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ConfigurableEnvironment environment;

    @Test
    public void shouldConnectToMemSQL() throws Exception {
        assertThat(jdbcTemplate.queryForObject("select @@version_comment", String.class)).contains("MemSQL");
        jdbcTemplate.execute("create table foo (id int primary key);");
        jdbcTemplate.execute("insert into foo values (1), (2), (3);");
        assertThat(jdbcTemplate.queryForList("select * from foo")).hasSize(3);

    }

    @Test
    public void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.memsql.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.memsql.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.memsql.schema")).isNotEmpty();
        assertThat(environment.getProperty("embedded.memsql.user")).isNotEmpty();
        assertThat(environment.getProperty("embedded.memsql.password")).isEqualTo("");
    }
}
