package com.playtika.test.common.utils;

import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.playtika.test.bootstrap.EchoContainer;
import com.playtika.test.common.checks.PositiveCommandWaitStrategy;
import com.playtika.test.common.properties.CommonContainerProperties;
import com.playtika.test.common.properties.CommonContainerProperties.CopyFileProperties;
import com.playtika.test.common.properties.CommonContainerProperties.MountVolume;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.utility.MountableFile;

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
    void configureCommonsAndStart() {
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
        Condition<Map.Entry<MountableFile, String>> hasCopyToFileContainerPath = new Condition<Map.Entry<MountableFile, String>>() {
            public boolean matches(Map.Entry<MountableFile, String> mountableFileObjectEntry) {
                return mountableFileObjectEntry.getKey().getResolvedPath().endsWith(classpathResource)
                        && mountableFileObjectEntry.getValue().equals(containerPath);
            }
        };
        assertThat(echoContainer.getCopyToFileContainerPathMap()).hasEntrySatisfying(hasCopyToFileContainerPath);

        for (MountVolume mountVolume : mountVolumes) {
            Condition<Bind> hasMountBindings = new Condition<>(
                bind -> MountableFile.forHostPath(mountVolume.getHostPath()).getResolvedPath().equals(bind.getPath())
                    && mountVolume.getContainerPath().equals(bind.getVolume().getPath())
                    && (BindMode.READ_WRITE.equals(mountVolume.getMode()) && AccessMode.rw == bind.getAccessMode()
                    || BindMode.READ_ONLY.equals(mountVolume.getMode()) && AccessMode.ro == bind.getAccessMode()), "binding");
            assertThat(echoContainer.getBinds()).hasSize(2).haveExactly(1, hasMountBindings);
        }
    }
}
