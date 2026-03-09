package com.playtika.testcontainer.mssqlserver;

import org.testcontainers.mssqlserver.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

public class EmbeddedMSSQLServerContainer extends MSSQLServerContainer {

    public EmbeddedMSSQLServerContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
    }

    public EmbeddedMSSQLServerContainer(String dockerImageName) {
        super(dockerImageName);
    }
}
