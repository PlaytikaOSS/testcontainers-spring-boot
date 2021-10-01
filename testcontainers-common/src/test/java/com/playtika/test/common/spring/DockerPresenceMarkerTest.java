package com.playtika.test.common.spring;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertThrows;

class DockerPresenceMarkerTest {

    @Test
    void markerShouldBlockContextIfDockerIsAbsent() {
        assertThrows(DockerNotPresentException.class, () -> new DockerPresenceMarker(false));
    }
}