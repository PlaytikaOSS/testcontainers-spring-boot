package com.playtika.testcontainers.aerospike.enterprise;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ImageVersionTest {

    @Test
    void shouldParseImageVersion() {
        ImageVersion parsedVersion = ImageVersion.parse("5.66.4.43_4");
        assertThat(parsedVersion).isEqualTo(new ImageVersion(5, 66));
    }

    @Test
    void shouldThrowIllegalStateException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> ImageVersion.parse("5"));

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> ImageVersion.parse("5_.6"));

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> ImageVersion.parse("5.6_"));
    }

    @Test
    void shouldCompareImageVersion() {
        assertThat(new ImageVersion(5, 66)).isLessThan(new ImageVersion(6, 66));
        assertThat(new ImageVersion(5, 66)).isLessThan(new ImageVersion(5, 67));

        assertThat(new ImageVersion(5, 66)).isGreaterThan(new ImageVersion(5, 30));
        assertThat(new ImageVersion(5, 66)).isGreaterThan(new ImageVersion(4, 67));

        assertThat(new ImageVersion(5, 66)).isEqualByComparingTo(new ImageVersion(5, 66));
    }
}
