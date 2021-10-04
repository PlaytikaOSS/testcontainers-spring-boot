package com.playtika.test.dynamodb;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

import static nl.jqno.equalsverifier.Warning.NONFINAL_FIELDS;
import static nl.jqno.equalsverifier.Warning.STRICT_INHERITANCE;

public class DynamodbConfigurationPropertiesTest {

    @Test
    public void shouldHave_equalsAndHashcodeContract() {
        EqualsVerifier.forClass(DynamoDBProperties.class)
                .suppress(NONFINAL_FIELDS, STRICT_INHERITANCE)
                .withRedefinedSuperclass()
                .verify();
    }
}