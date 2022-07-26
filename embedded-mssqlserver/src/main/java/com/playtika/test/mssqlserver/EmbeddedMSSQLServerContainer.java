package com.playtika.test.mssqlserver;

import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

public class EmbeddedMSSQLServerContainer extends MSSQLServerContainer<EmbeddedMSSQLServerContainer> {

    public EmbeddedMSSQLServerContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
    }

    public EmbeddedMSSQLServerContainer(String dockerImageName) {
        super(dockerImageName);
    }
}
