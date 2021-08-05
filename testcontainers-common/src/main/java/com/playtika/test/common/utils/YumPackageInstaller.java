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
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

public class YumPackageInstaller extends PackageInstaller {

    public YumPackageInstaller(InstallPackageProperties properties, GenericContainer container) {
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
