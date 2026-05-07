package com.playtika.testcontainer.common.utils;

import com.playtika.testcontainer.bootstrap.EchoContainer;
import com.playtika.testcontainer.common.checks.PositiveCommandWaitStrategy;
import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import com.playtika.testcontainer.common.properties.CommonContainerProperties.CopyFileProperties;
import com.playtika.testcontainer.common.properties.CommonContainerProperties.MountVolume;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.MountableFile;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class ContainerUtilsTest {

    EchoContainer echoContainer;

    @BeforeEach
    void setUp() {
        echoContainer = new EchoContainer().waitingFor(new PositiveCommandWaitStrategy());
    }

    @AfterEach
    void tearDown() {
        echoContainer.stop();
    }

    @Test
    void configureCommonsAndStart() throws Exception {
        String[] command = {"/bin/sh", "-c", "while true; do echo 'Press [CTRL+C] to stop..'; sleep 1; done"};
        Map<String, String> env = new HashMap<>();
        env.put("TEST_ENV_VAR", "VALUE_TEST");
        env.put("TEST_ENV_VAR_2", "some other value");

        String classpathResource = "/log4j2.xml";
        String containerPath = "/etc/my_copied_file";
        List<MountVolume> mountVolumes = new ArrayList<>();
        mountVolumes.add(new MountVolume("pgdata", "/var/lib/postgresql/data", BindMode.READ_WRITE));
        mountVolumes.add(new MountVolume("src/main/resources/my-postgresql.conf", "/etc/postgresql/postgresql.conf", BindMode.READ_ONLY));

        CommonContainerProperties commonContainerProperties = new CommonContainerProperties() {
            @Override
            public String getDefaultDockerImage() {
                return null;
            }
        };
        commonContainerProperties.setCommand(command);
        commonContainerProperties.setReuseContainer(true);
        commonContainerProperties.setEnv(env);

        commonContainerProperties.setFilesToInclude(singletonList(new CopyFileProperties(classpathResource, containerPath)));
        commonContainerProperties.setMountVolumes(mountVolumes);

        echoContainer = (EchoContainer) ContainerUtils.configureCommonsAndStart(echoContainer, commonContainerProperties, log);

        assertThat(echoContainer.isRunning()).isTrue();
        assertThat(echoContainer.getCommandParts()).isEqualTo(command);
        assertThat(echoContainer.isShouldBeReused()).isTrue();
        assertThat(echoContainer.getEnvMap()).containsAllEntriesOf(env);
        assertThat(echoContainer.getLogConsumers()).hasSize(1);
        Condition<Map.Entry<MountableFile, String>> hasCopyToFileContainerPath = new Condition<>() {
            public boolean matches(Map.Entry<MountableFile, String> mountableFileObjectEntry) {
                return mountableFileObjectEntry.getKey().getResolvedPath().endsWith(classpathResource)
                       && mountableFileObjectEntry.getValue().equals(containerPath);
            }
        };
        assertThat(echoContainer.getCopyToFileContainerPathMap()).hasEntrySatisfying(hasCopyToFileContainerPath);

        Map<Transferable, String> transferableMap = getCopyToTransferableContainerPathMap(echoContainer);
        assertThat(transferableMap).hasSize(2);
        for (MountVolume mountVolume : mountVolumes) {
            String expectedPath = MountableFile.forHostPath(mountVolume.getHostPath()).getResolvedPath();
            Condition<Map.Entry<Transferable, String>> hasMountVolume = new Condition<>(
                entry -> entry.getKey() instanceof MountableFile mountableFile
                    && mountableFile.getResolvedPath().equals(expectedPath)
                    && mountVolume.getContainerPath().equals(entry.getValue()),
                "mount volume copy");
            assertThat(transferableMap).hasEntrySatisfying(hasMountVolume);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<Transferable, String> getCopyToTransferableContainerPathMap(EchoContainer container) throws Exception {
        Field field = GenericContainer.class.getDeclaredField("copyToTransferableContainerPathMap");
        field.setAccessible(true);
        return (Map<Transferable, String>) field.get(container);
    }
}
