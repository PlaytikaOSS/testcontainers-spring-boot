package com.playtika.testcontainer.common.utils;

import com.playtika.testcontainer.common.properties.InstallPackageProperties;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class YumPackageInstallerTest {

    @Test
    public void shouldInstallReturnsTrueWhenPackageMissing() {
        InstallPackageProperties properties = new InstallPackageProperties();
        GenericContainer<?> container = mock(GenericContainer.class);

        TestableYumPackageInstaller installer = new TestableYumPackageInstaller(properties, container);
        installer.setMockExitCode(1); // Package missing

        assertThat(installer.shouldInstall("missing-package")).isTrue();
        assertThat(installer.lastCommand).containsExactly("rpm", "-q", "missing-package");
    }

    @Test
    public void shouldInstallReturnsFalseWhenPackagePresent() {
        InstallPackageProperties properties = new InstallPackageProperties();
        GenericContainer<?> container = mock(GenericContainer.class);

        TestableYumPackageInstaller installer = new TestableYumPackageInstaller(properties, container);
        installer.setMockExitCode(0); // Package present

        assertThat(installer.shouldInstall("present-package")).isFalse();
        assertThat(installer.lastCommand).containsExactly("rpm", "-q", "present-package");
    }

    static class TestableYumPackageInstaller extends YumPackageInstaller {
        private int mockExitCode;
        public String[] lastCommand;

        public TestableYumPackageInstaller(InstallPackageProperties properties, GenericContainer<?> container) {
            super(properties, container);
        }

        public void setMockExitCode(int code) {
            this.mockExitCode = code;
        }

        @Override
        public boolean shouldInstall(String packageToInstall) {
             return super.shouldInstall(packageToInstall);
        }

        @Override
        protected Container.ExecResult executeCommand(String... command) {
            this.lastCommand = command;
            Container.ExecResult result = mock(Container.ExecResult.class);
            when(result.getExitCode()).thenReturn(mockExitCode);
            when(result.getStdout()).thenReturn("mock output");
            return result;
        }
    }
}
