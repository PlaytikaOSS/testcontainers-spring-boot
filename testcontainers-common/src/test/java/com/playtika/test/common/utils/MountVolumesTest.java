package com.playtika.test.common.utils;

import com.playtika.test.common.properties.CommonContainerProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.BindMode;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
public class MountVolumesTest {

    ApplicationContextRunner context = new ApplicationContextRunner()
        .withUserConfiguration(PostgreSQLContainerPropertiesConfiguration.class);

    private static void assertMountVolume(CommonContainerProperties properties, String folder, String s, BindMode readOnly) {
        assertThat(properties).isNotNull();
        assertThat(properties.getMountVolumes()).hasSize(1);
        assertThat(properties.getMountVolumes().get(0).getHostPath()).isEqualTo(folder);
        assertThat(properties.getMountVolumes().get(0).getContainerPath()).isEqualTo(s);
        assertThat(properties.getMountVolumes().get(0).getMode()).isEqualTo(readOnly);
    }

    @Test
    void noConfigurationEqualsEmptyList() {
        context.run(it -> {
            assertThat(it).hasNotFailed();
            assertThat(it).hasSingleBean(CommonContainerProperties.class);
            CommonContainerProperties properties = it.getBean(CommonContainerProperties.class);
            assertThat(properties).isNotNull();
            assertThat(properties.getMountVolumes()).hasSize(0);
        });
    }

    @Test
    void partialConfigurationEqualsDefaultList() {
        context
            .withPropertyValues("embedded.postgresql.mount-volumes[0].host-path=folder", "embedded.postgresql.mount-volumes[0].container-path=/mnt/folder")
            .run(it -> {
                assertThat(it).hasNotFailed();
                assertThat(it).hasSingleBean(CommonContainerProperties.class);
                CommonContainerProperties properties = it.getBean(CommonContainerProperties.class);
                assertMountVolume(properties, "folder", "/mnt/folder", BindMode.READ_ONLY);
            });
    }

    @Test
    void propertyNamesWorkForAllSpringBootRelaxations() {
        context
            .withPropertyValues("embedded.postgresql.MOUNTVOLUMES[0].MODE=Read_WRITE", "embedded.postgresql.mount-Volumes[0].host-PATH=folder", "embedded.postgresql.MOUNT-volumes[0].containerPath=/mnt/folder")
            .run(it -> {
                assertThat(it).hasNotFailed();
                assertThat(it).hasSingleBean(CommonContainerProperties.class);
                CommonContainerProperties properties = it.getBean(CommonContainerProperties.class);
                assertMountVolume(properties, "folder", "/mnt/folder", BindMode.READ_WRITE);
            });
    }

    @Test
    void wrongModeFailsAtStartup() {
        context.withPropertyValues("embedded.postgresql.mountVolumes[0].mode=read_Wrong")
               .run(it -> {
                   assertThat(it).hasFailed();
                   assertThat(it).getFailure().hasCauseExactlyInstanceOf(BindException.class);
                   assertThat(it).getFailure().getCause()
                                 .hasMessage("Failed to bind properties under 'embedded.postgresql.mount-volumes[0].mode' to org.testcontainers.containers.BindMode");
               });
    }

    @Test
    void nullContainerPathFailsAtStartup() {
        context.withPropertyValues("embedded.postgresql.mount-volumes[0].host-path=folder")
               .run(it -> {
                   assertThat(it).hasFailed();
                   assertThat(it).getFailure().hasRootCauseExactlyInstanceOf(BindValidationException.class);
                   assertThat(it).getFailure().getRootCause()
                                 .hasMessageContaining("on field 'containerPath': rejected value [null];");
               });
    }

    @Test
    void emptyContainerPathFailsAtStartup() {
        context
            .withPropertyValues("embedded.postgresql.mount-volumes[0].host-path=folder", "embedded.postgresql.mount-volumes[0].container-path= ")
            .run(it -> {
                assertThat(it).hasFailed();
                assertThat(it).getFailure().hasRootCauseExactlyInstanceOf(BindValidationException.class)
                              .getRootCause().hasMessageContaining("on field 'containerPath': rejected value [];");
            });
    }

    @Test
    void nullHostPathFailsAtStartup() {
        context.withPropertyValues("embedded.postgresql.mount-volumes[0].container-path=folder")
               .run(it -> {
                   assertThat(it).hasFailed();
                   assertThat(it).getFailure().hasRootCauseExactlyInstanceOf(BindValidationException.class)
                                 .getRootCause().hasMessageContaining("on field 'hostPath': rejected value [null];");
               });
    }

    @Test
    void emptyHostPathFailsAtStartup() {
        context
            .withPropertyValues("embedded.postgresql.mount-volumes[0].host-path= ", "embedded.postgresql.mount-volumes[0].container-path=/mnt/folder")
            .run(it -> {
                assertThat(it).hasFailed();
                assertThat(it).getFailure().hasRootCauseExactlyInstanceOf(BindValidationException.class)
                              .getRootCause().hasMessageContaining("on field 'hostPath': rejected value [];");
            });
    }

    @Test
    void inheritedPropertiesWorkIndependently() {
        context.withUserConfiguration(OtherContainerPropertiesConfiguration.class)
               .withPropertyValues("embedded.postgresql.mount-volumes[0].host-path=folder", "embedded.postgresql.mount-volumes[0].container-path=/mnt/folder", "embedded.postgresql.mount-volumes[0].mode=READ_WRITE")
               .withPropertyValues("embedded.other.mount-Volumes[0].host-PATH=folder2", "embedded.other.MOUNT-volumes[0].containerPath=/mnt/folder2")
               .run(it -> {
                   assertThat(it).hasNotFailed();
                   Map<String, CommonContainerProperties> commonContainerPropertiesMap = it.getBeansOfType(CommonContainerProperties.class);
                   assertThat(commonContainerPropertiesMap)
                       .hasSize(2)
                       .containsKey("embedded.postgresql-" + PostgreSQLContainerProperties.class.getName())
                       .containsKey("embedded.other-" + OtherContainerProperties.class.getName());
                   assertThat(commonContainerPropertiesMap
                       .get("embedded.postgresql-" + PostgreSQLContainerProperties.class.getName()))
                       .isExactlyInstanceOf(PostgreSQLContainerProperties.class);
                   assertThat(commonContainerPropertiesMap
                       .get("embedded.other-" + OtherContainerProperties.class.getName()))
                       .isExactlyInstanceOf(OtherContainerProperties.class);

                   CommonContainerProperties properties = commonContainerPropertiesMap
                       .get("embedded.postgresql-" + PostgreSQLContainerProperties.class.getName());
                   assertMountVolume(properties, "folder", "/mnt/folder", BindMode.READ_WRITE);

                   assertThat(it).hasSingleBean(OtherContainerProperties.class);
                   OtherContainerProperties inheritedProperties = it.getBean(OtherContainerProperties.class);
                   assertThat(commonContainerPropertiesMap
                       .get("embedded.other-" + OtherContainerProperties.class.getName()))
                       .isSameAs(inheritedProperties);
                   assertMountVolume(inheritedProperties, "folder2", "/mnt/folder2", BindMode.READ_ONLY);
               });
    }

    @TestConfiguration
    @EnableConfigurationProperties(PostgreSQLContainerProperties.class)
    static class PostgreSQLContainerPropertiesConfiguration {
    }

    @ConfigurationProperties("embedded.postgresql")
    static class PostgreSQLContainerProperties extends CommonContainerProperties {
    }

    @TestConfiguration
    @EnableConfigurationProperties(OtherContainerProperties.class)
    static class OtherContainerPropertiesConfiguration {
    }

    @ConfigurationProperties("embedded.other")
    static class OtherContainerProperties extends CommonContainerProperties {
    }

}
