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
    public void shouldExecuteSelectDataFromInitScriptClickHouse() throws Exception {
        assertThat(jdbcTemplate.queryForObject("select first_name from test.users where id = 1", String.class)).isEqualTo("first_name_test");
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
