package com.playtika.test.kafka.properties;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static nl.jqno.equalsverifier.Warning.NONFINAL_FIELDS;
import static nl.jqno.equalsverifier.Warning.STRICT_INHERITANCE;

@DisplayName("Test that ZookeeperConfigurationProperties")
public class ZookeeperConfigurationPropertiesTest {

    @Test
    @DisplayName("respects contract for equals & hashCode")
    public void shouldRespectEqualsAndHashcodeContract() {
        EqualsVerifier
                .forClass(ZookeeperConfigurationProperties.class)
                .suppress(NONFINAL_FIELDS, STRICT_INHERITANCE)
                .withRedefinedSuperclass()
                .verify();
    }
}