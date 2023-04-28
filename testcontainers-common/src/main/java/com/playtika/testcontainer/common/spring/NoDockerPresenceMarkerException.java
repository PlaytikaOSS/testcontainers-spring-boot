package com.playtika.testcontainer.common.spring;

public class NoDockerPresenceMarkerException extends IllegalStateException {
    public NoDockerPresenceMarkerException(String s) {
        super(s);
    }
}
