package com.playtika.test.couchbase;


import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

public class CouchbasePropertiesTest {

    @Test
    public void shouldFailIfPasswordHas5Chars() {
        CouchbaseProperties couchbaseProperties = new CouchbaseProperties();
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> couchbaseProperties.setPassword("12345"));
    }

    @Test
    public void shouldNotFailIfPasswordHas6Chars() {
        CouchbaseProperties couchbaseProperties = new CouchbaseProperties();
        assertThatCode(() -> couchbaseProperties.setPassword("123456")).doesNotThrowAnyException();
    }
}