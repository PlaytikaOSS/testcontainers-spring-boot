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
package com.playtika.test.clickhouse;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.playtika.test.clickhouse.ClickHouseProperties.DEFAULT_DOCKER_IMAGE_TAG;
import static org.assertj.core.api.Assertions.assertThat;

public class EmbeddedClickHouseBootstrapConfigurationTest extends BaseEmbeddedClickHouseTest {

    @Test
    public void shouldConnectToClickHouse() throws Exception {
        Matcher m = Pattern.compile("([0-9]+\\.[0-9]+).*").matcher(DEFAULT_DOCKER_IMAGE_TAG);
        String versionPrefix = m.matches() ? m.toMatchResult().group(1) + "." : "21.7."; // Fallback to last known version
        assertThat(jdbcTemplate.queryForObject("select version()", String.class)).startsWith(versionPrefix);
    }

    @Test
    public void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.clickhouse.schema")).isNotEmpty();
        assertThat(environment.getProperty("embedded.clickhouse.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.clickhouse.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.clickhouse.user")).isNotEmpty();
        assertThat(environment.getProperty("embedded.clickhouse.password")).isNotNull();
    }
}
