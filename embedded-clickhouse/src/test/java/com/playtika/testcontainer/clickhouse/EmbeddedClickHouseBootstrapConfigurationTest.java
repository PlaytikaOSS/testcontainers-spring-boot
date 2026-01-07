package com.playtika.testcontainer.clickhouse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import static org.assertj.core.api.Assertions.assertThat;

public class EmbeddedClickHouseBootstrapConfigurationTest extends BaseEmbeddedClickHouseTest {

    @Value("${embedded.clickhouse.schema}")
    String schema;

    @Value("${embedded.clickhouse.host}")
    String host;

    @Value("${embedded.clickhouse.port}")
    String port;

    @Value("${embedded.clickhouse.user}")
    String user;

    @Value("${embedded.clickhouse.password}")
    String password;

    @Test
    public void shouldExecuteSelectDataFromInitScriptClickHouse() throws Exception {
        assertThat(jdbcTemplate.queryForObject("select first_name from test.users where id = 1", String.class)).isEqualTo("first_name_test");
    }

    @Test
    public void propertiesAreAvailable() {
        assertThat(schema).isNotEmpty();
        assertThat(host).isNotEmpty();
        assertThat(port).isNotEmpty();
        assertThat(user).isNotEmpty();
        assertThat(password).isNotNull();
    }
}
