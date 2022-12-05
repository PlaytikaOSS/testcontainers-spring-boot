package com.playtika.test.common.utils;

import com.playtika.test.common.properties.InstallPackageProperties;
import org.testcontainers.containers.GenericContainer;


public class AptGetPackageInstaller extends PackageInstaller {

    public AptGetPackageInstaller(InstallPackageProperties properties, GenericContainer<?> container) {
        super(properties, container);
    }

    @Override
    protected void updatePackageList() {
        executeCommandAndCheckExitCode("apt-get", "update");
    }

    @Override
    protected void install(String packageToInstall) {
        executeCommandAndCheckExitCode("apt-get", "-qq", "-y", "install", packageToInstall);
    }
}
