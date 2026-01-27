package com.playtika.testcontainer.common.utils;

import com.playtika.testcontainer.common.properties.InstallPackageProperties;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

public class YumPackageInstaller extends PackageInstaller {

    public YumPackageInstaller(InstallPackageProperties properties, GenericContainer<?> container) {
        super(properties, container);
    }

    @Override
    protected boolean shouldInstall(String packageToInstall) {
        Container.ExecResult execResult = executeCommand("rpm", "-q", packageToInstall);
        return execResult.getExitCode() != 0;
    }

    @Override
    protected void install(String packageToInstall) {
        executeCommandAndCheckExitCode("yum", "-y", "install", packageToInstall);
    }
}
