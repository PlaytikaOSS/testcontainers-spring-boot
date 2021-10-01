package com.playtika.test.dynamodb;

import static nl.jqno.equalsverifier.Warning.NONFINAL_FIELDS;
import static nl.jqno.equalsverifier.Warning.STRICT_INHERITANCE;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

public class DynamodbConfigurationPropertiesTest {

    @Test
    public void shouldHave_equalsAndHashcodeContract() {
        EqualsVerifier.forClass(DynamoDBProperties.class)
                .suppress(NONFINAL_FIELDS, STRICT_INHERITANCE)
                .withRedefinedSuperclass()
                .verify();
    }
}