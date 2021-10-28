package com.playtika.test.common.spring;

public class NoDockerPresenceMarkerException extends IllegalStateException {
    public NoDockerPresenceMarkerException(String s) {
        super(s);
    }
}
