package com.playtika.testcontainer.common.utils;

import com.playtika.testcontainer.common.properties.InstallPackageProperties;
import org.testcontainers.containers.GenericContainer;

public class MicroDnfPackageInstaller extends PackageInstaller {

    public MicroDnfPackageInstaller(InstallPackageProperties properties, GenericContainer<?> container) {
        super(properties, container);
    }

    @Override
    protected void updatePackageList() {
        executeCommandAndCheckExitCode("microdnf", "update");
    }

    @Override
    protected boolean shouldInstall(String packageToInstall) {
        return true;
    }

    //https://www.mankier.com/8/microdnf
    @Override
    protected void install(String packageToInstall) {
        executeCommandAndCheckExitCode("microdnf", "-y", "install", packageToInstall);
    }
}
