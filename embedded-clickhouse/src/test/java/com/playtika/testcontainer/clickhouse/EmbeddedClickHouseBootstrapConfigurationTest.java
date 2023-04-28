package com.playtika.testcontainer.clickhouse;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EmbeddedClickHouseBootstrapConfigurationTest extends BaseEmbeddedClickHouseTest {

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
