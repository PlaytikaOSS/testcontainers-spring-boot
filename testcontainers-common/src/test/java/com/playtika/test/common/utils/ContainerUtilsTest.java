package com.playtika.test.common.utils;

import com.playtika.test.bootstrap.EchoContainer;
import com.playtika.test.common.checks.PositiveCommandWaitStrategy;
import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import org.testcontainers.utility.MountableFile;

import java.util.Map;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class ContainerUtilsTest {

    EchoContainer echoContainer;

    @BeforeEach
    void setUp() {
        echoContainer = new EchoContainer()
                .waitingFor(new PositiveCommandWaitStrategy());
    }

    @AfterEach
    void tearDown() {
        echoContainer.stop();
    }

    @Test
    void configureCommonsAndStart() {
        String[] command = {"/bin/sh", "-c", "while true; do echo 'Press [CTRL+C] to stop..'; sleep 1; done"};
        Map<String, String> env = ImmutableMap.of(
                "TEST_ENV_VAR", "VALUE_TEST",
                "TEST_ENV_VAR_2", "some other value");
        String classpathResource = "/log4j2.xml";
        String containerPath = "/etc/my_copied_file";

        CommonContainerProperties commonContainerProperties = new CommonContainerProperties();
        commonContainerProperties.setCommand(command);
        commonContainerProperties.setReuseContainer(true);
        commonContainerProperties.setEnv(env);

        CommonContainerProperties.CopyFileProperties copyFileProperties = new CommonContainerProperties.CopyFileProperties();
        copyFileProperties.setClasspathResource(classpathResource);
        copyFileProperties.setContainerPath(containerPath);
        commonContainerProperties.setFilesToInclude(singletonList(copyFileProperties));

        echoContainer = (EchoContainer) ContainerUtils.configureCommonsAndStart(echoContainer, commonContainerProperties, log);

        assertThat(echoContainer.isRunning()).isTrue();
        assertThat(echoContainer.getCommandParts()).isEqualTo(command);
        assertThat(echoContainer.isShouldBeReused()).isTrue();
        assertThat(echoContainer.getEnvMap()).containsAllEntriesOf(env);
        assertThat(echoContainer.getLogConsumers()).hasSize(1);
        Condition<Map.Entry<MountableFile, String>> hasCopyToFileContainerPath = new Condition<Map.Entry<MountableFile, String>>() {
            public boolean matches(Map.Entry<MountableFile, String> mountableFileObjectEntry) {
                return mountableFileObjectEntry.getKey().getResolvedPath().endsWith(classpathResource)
                        && mountableFileObjectEntry.getValue().equals(containerPath);
            }
        };
        assertThat(echoContainer.getCopyToFileContainerPathMap()).hasEntrySatisfying(hasCopyToFileContainerPath);
    }
}