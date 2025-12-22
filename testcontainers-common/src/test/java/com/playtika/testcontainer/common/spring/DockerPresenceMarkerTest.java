package com.playtika.testcontainer.common.spring;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class DockerPresenceMarkerTest {

    @Test
    void markerShouldBlockContextIfDockerIsAbsent() {
        assertThrows(DockerNotPresentException.class, () -> new DockerPresenceMarker(false));
    }
}