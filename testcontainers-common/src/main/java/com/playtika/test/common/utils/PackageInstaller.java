/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Playtika
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.playtika.test.common.utils;

import com.playtika.test.common.properties.InstallPackageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import javax.annotation.PostConstruct;

@Slf4j
@RequiredArgsConstructor
public abstract class PackageInstaller {

    private final InstallPackageProperties properties;
    private final GenericContainer container;

    protected abstract void install(String packageToInstall);

    @PostConstruct
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

    protected void executeCommandAndCheckExitCode(String... command) {
        ContainerUtils.executeAndCheckExitCode(container, command);
    }

}
