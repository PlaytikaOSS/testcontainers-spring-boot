package com.playtika.testcontainer.common.spring;

import lombok.Value;

@Value
public class DockerPresenceMarker {

    boolean dockerPresent;

    public DockerPresenceMarker(boolean dockerPresent) {
        if(!dockerPresent){
            throw new DockerNotPresentException("Docker must be present in order for testcontainers to work properly!");
        }
        this.dockerPresent = dockerPresent;
    }
}
