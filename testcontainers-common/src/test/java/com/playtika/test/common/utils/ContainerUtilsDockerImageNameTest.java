package com.playtika.test.common.utils;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

public class ContainerUtilsDockerImageNameTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(UserConfigurations.of(TestConfig.class));

    @Test
    void noOverrides() {
        contextRunner.run((context) -> {
            assertThat(context).hasNotFailed();
            CommonContainerPropertiesWithDefault properties = context.getBean(CommonContainerPropertiesWithDefault.class);

            assertThat(ContainerUtils.getDockerImageName(properties)).isEqualTo(DockerImageName.parse("some-default-image:123"));
        });
    }

    @Test
    void overriddenDockerImage() {
        contextRunner.withPropertyValues(
                "embedded.testdb.dockerImage=my-custom-image:1.1.1"
        ).run((context) -> {
            assertThat(context).hasNotFailed();
            CommonContainerPropertiesWithDefault properties = context.getBean(CommonContainerPropertiesWithDefault.class);

            assertThat(properties.getDockerImage()).isEqualTo("my-custom-image:1.1.1");
            assertThat(ContainerUtils.getDockerImageName(properties)).isEqualTo(DockerImageName.parse("my-custom-image:1.1.1"));
        });
    }

    @Test
    void overriddenDockerImageVersion() {
        contextRunner.withPropertyValues(
                "embedded.testdb.dockerImageVersion=1.1.1"
        ).run((context) -> {
            assertThat(context).hasNotFailed();
            CommonContainerPropertiesWithDefault properties = context.getBean(CommonContainerPropertiesWithDefault.class);

            assertThat(ContainerUtils.getDockerImageName(properties)).isEqualTo(DockerImageName.parse("some-default-image:1.1.1"));
        });
    }

    @Test
    void overriddenDockerImageAndDockerImageName() {
        contextRunner.withPropertyValues(
                "embedded.testdb.dockerImage=my-custom-image",
                "embedded.testdb.dockerImageVersion=5.8.0"
        ).run((context) -> {
            assertThat(context).hasNotFailed();
            CommonContainerPropertiesWithDefault properties = context.getBean(CommonContainerPropertiesWithDefault.class);

            assertThat(ContainerUtils.getDockerImageName(properties)).isEqualTo(DockerImageName.parse("my-custom-image:5.8.0"));
        });
    }

    @Test
    void overriddenDockerImageWithNoDefaultDockerImage() {
        contextRunner.withPropertyValues(
                "embedded.testdb2.dockerImage=my-custom-image:5.8.0"
        ).run((context) -> {
            assertThat(context).hasNotFailed();
            CommonContainerPropertiesNoDefault properties = context.getBean(CommonContainerPropertiesNoDefault.class);

            assertThat(ContainerUtils.getDockerImageName(properties)).isEqualTo(DockerImageName.parse("my-custom-image:5.8.0"));
        });
    }

    @EnableConfigurationProperties
    @Configuration
    static class TestConfig {

        @Bean
        CommonContainerPropertiesWithDefault commonContainerPropertiesWithDefault() {
            return new CommonContainerPropertiesWithDefault();
        }

        @Bean
        CommonContainerPropertiesNoDefault commonContainerPropertiesNoDefault() {
            return new CommonContainerPropertiesNoDefault();
        }

    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @ConfigurationProperties("embedded.testdb")
    public static class CommonContainerPropertiesWithDefault extends CommonContainerProperties {

        public CommonContainerPropertiesWithDefault() {
        }

        @Override
        public String getDefaultDockerImage() {
            return "some-default-image:123";
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @ConfigurationProperties("embedded.testdb2")
    public static class CommonContainerPropertiesNoDefault extends CommonContainerProperties {

        public CommonContainerPropertiesNoDefault() {
        }

        @Override
        public String getDefaultDockerImage() {
            return null;
        }
    }
}
