package com.playtika.test.vertica;

import lombok.extern.slf4j.Slf4j;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

import static nl.jqno.equalsverifier.Warning.NONFINAL_FIELDS;
import static nl.jqno.equalsverifier.Warning.STRICT_INHERITANCE;

@Slf4j
public class VerticaConfigurationPropertiesTest {

    @Test
    public void shouldHaveEqualsAndHashcodeContract() {
        EqualsVerifier.forClass(VerticaProperties.class)
                .suppress(NONFINAL_FIELDS, STRICT_INHERITANCE)
                .withRedefinedSuperclass()
                .verify();
    }
}
