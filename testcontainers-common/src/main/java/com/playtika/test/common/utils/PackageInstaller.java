package com.playtika.test.common.utils;

import com.playtika.test.common.properties.InstallPackageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

/**
 * Instead use ToxiPoxy.
 */
@Slf4j
@RequiredArgsConstructor
@Deprecated
public abstract class PackageInstaller implements InitializingBean {

    private final InstallPackageProperties properties;
    private final GenericContainer<?> container;

    protected abstract void install(String packageToInstall);

    @Override
    public void afterPropertiesSet() {
        installPackages();
    }

    protected void installPackages() {
        String dockerImageName = container.getDockerImageName();
        String containerName = container.getContainerInfo().getName();
        if (!properties.isEnabled()) {
            log.trace("Packages installation skipped for container: {} docker image: {}", containerName, dockerImageName);
            return;
        }
        if (properties.getPackages().isEmpty()) {
            log.trace("No packages configured to be installed into container: {} docker image: {}", containerName, dockerImageName);
            return;
        }

        log.info("Updating package lists in container: {} docker image: {}", containerName, dockerImageName);
        updatePackageList();
        log.info("Installing packages: {} into container: {} docker image: {}", properties.getPackages(), containerName, dockerImageName);
        properties.getPackages().forEach(this::installPackageIfNeeded);
        log.info("Installed packages: {} into container: {} docker image: {}", properties.getPackages(), containerName, dockerImageName);
    }

    protected void updatePackageList() {
        //not required
    }

    protected void installPackageIfNeeded(String packageToInstall) {
        if (shouldInstall(packageToInstall)) {
            install(packageToInstall);
        }
    }

    protected boolean shouldInstall(String packageToInstall) {
        Container.ExecResult result = ContainerUtils.executeInContainer(container, "which", packageToInstall);
        // returns empty result if package is not installed
        // if package is installed -- returns path
        // see: https://www.ostechnix.com/how-to-find-if-a-package-is-installed-or-not-in-linux-and-unix/
        return result.getStdout().isEmpty();
    }

    protected Container.ExecResult executeCommandAndCheckExitCode(String... command) {
        return ContainerUtils.executeAndCheckExitCode(container, command);
    }

}
