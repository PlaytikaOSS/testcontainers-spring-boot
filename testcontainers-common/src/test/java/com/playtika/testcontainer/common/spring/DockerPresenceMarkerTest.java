package com.playtika.testcontainer.common.spring;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class DockerPresenceMarkerTest {

    @Test
    void markerShouldBlockContextIfDockerIsAbsent() {
        assertThatExceptionOfType(DockerNotPresentException.class).isThrownBy(() -> new DockerPresenceMarker(false));
    }
}
