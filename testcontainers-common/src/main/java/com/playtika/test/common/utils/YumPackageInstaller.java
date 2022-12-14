package com.playtika.test.common.utils;

import com.playtika.test.common.properties.InstallPackageProperties;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

public class YumPackageInstaller extends PackageInstaller {

    public YumPackageInstaller(InstallPackageProperties properties, GenericContainer<?> container) {
        super(properties, container);
    }

    @Override
    protected boolean shouldInstall(String packageToInstall) {
        Container.ExecResult execResult = executeCommandAndCheckExitCode("yum", "list", "installed");
        return !execResult.getStdout().contains(packageToInstall);
    }

    @Override
    protected void install(String packageToInstall) {
        executeCommandAndCheckExitCode("yum", "-y", "install", packageToInstall);
    }
}
