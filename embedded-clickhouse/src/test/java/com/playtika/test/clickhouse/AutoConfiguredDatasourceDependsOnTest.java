package com.playtika.test.clickhouse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactoryUtils;

import javax.sql.DataSource;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class AutoConfiguredDatasourceDependsOnTest extends BaseEmbeddedClickHouseTest {

    @Test
    public void shouldSetupDependsOnForAllDataSources() throws Exception {
        String[] beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, DataSource.class);
        assertThat(beanNamesForType)
                .as("Auto-configured datasource should be present")
                .hasSize(1)
                .contains("dataSource");
        asList(beanNamesForType).forEach(this::hasDependsOn);
    }

}
