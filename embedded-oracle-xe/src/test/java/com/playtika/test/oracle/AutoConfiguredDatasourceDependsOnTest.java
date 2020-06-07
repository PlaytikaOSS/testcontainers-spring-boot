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
package com.playtika.test.oracle;

import static com.playtika.test.oracle.OracleProperties.BEAN_NAME_EMBEDDED_ORACLE;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import com.playtika.test.oracle.dummy.TestApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

class AutoConfiguredDatasourceDependsOnTest {

    @ActiveProfiles("enabled")
    @SpringBootTest(classes = {TestApplication.class})
    @DisplayName("Default AutoConfigured Datasource")
    @Nested
    class TestDefaults {

        @Autowired
        protected ConfigurableListableBeanFactory beanFactory;

        @Autowired
        protected JdbcTemplate jdbcTemplate;

        @Test
        void shouldConnectToOracle() {
            assertThat(jdbcTemplate.queryForObject("select 1 from dual", String.class)).contains("1");
        }

        @Test
        void shouldSetupDependsOnForAllDataSources() {
            String[] beanNamesForType = beanFactory.getBeanNamesForType(DataSource.class);
            assertThat(beanNamesForType)
                    .as("Auto-configured datasource should be present")
                    .hasSize(1)
                    .contains("dataSource");
            asList(beanNamesForType).forEach(this::hasDependsOn);
        }

        private void hasDependsOn(String beanName) {
            assertThat(beanFactory.getBeanDefinition(beanName).getDependsOn())
                    .isNotNull()
                    .isNotEmpty()
                    .contains(BEAN_NAME_EMBEDDED_ORACLE);
        }
    }

    @TestPropertySource(properties = {
            "embedded.oracle.docker-image=oracleinanutshell/oracle-xe-11g"
    })
    @Nested
    @DisplayName("AutoConfigured Datasource with oracleinanutshell/oracle-xe-11g")
    class Oracle11 extends TestDefaults {
    }
}

