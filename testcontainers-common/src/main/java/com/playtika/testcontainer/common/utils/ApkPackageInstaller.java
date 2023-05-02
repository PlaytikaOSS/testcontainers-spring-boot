package com.playtika.testcontainer.common.utils;

import com.playtika.testcontainer.common.properties.InstallPackageProperties;
import org.testcontainers.containers.GenericContainer;

public class ApkPackageInstaller extends PackageInstaller {

    public ApkPackageInstaller(InstallPackageProperties properties, GenericContainer<?> container) {
        super(properties, container);
    }

    @Override
    protected void updatePackageList() {
        executeCommandAndCheckExitCode("apk", "update");
    }

    @Override
    protected void install(String packageToInstall) {
        executeCommandAndCheckExitCode("apk", "add", packageToInstall);
    }
}
