package com.playtika.test.common.utils;

import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Capability;
import com.playtika.test.common.properties.CommonContainerProperties;
import com.playtika.test.common.properties.CommonContainerProperties.CopyFileProperties;
import com.playtika.test.common.properties.CommonContainerProperties.MountVolume;
import com.playtika.test.common.properties.CommonContainerProperties.TmpFs.TmpFsMount;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.images.ImagePullPolicy;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@UtilityClass
public class ContainerUtils {

    public static DockerImageName getDockerImageName(CommonContainerProperties properties) {
        String customImageName = properties.getDockerImage();
        String defaultDockerImageName = properties.getDefaultDockerImage();
        if (customImageName == null && defaultDockerImageName == null) {
            throw new IllegalStateException("Please specify dockerImage for the container.");
        }
        if (customImageName == null) {
            return setupImage(defaultDockerImageName, properties);
        }
        DockerImageName customImage = setupImage(customImageName, properties);
        if (defaultDockerImageName == null) {
            return customImage;
        }
        DockerImageName defaultImage = DockerImageName.parse(defaultDockerImageName);
        log.warn("Custom Docker image {} configured for the container. Note that it may not be compatible with the default Docker image {}.",
                customImage, defaultImage);
        return customImage.asCompatibleSubstituteFor(defaultImage);
    }

    private static DockerImageName setupImage(String imageName, CommonContainerProperties properties) {
        DockerImageName image = DockerImageName.parse(imageName);
        if (properties.getDockerImageVersion() != null) {
            image = image.withTag(properties.getDockerImageVersion());
        }
        return image;
    }

    public static GenericContainer<?> configureCommonsAndStart(GenericContainer<?> container,
                                                               CommonContainerProperties properties,
                                                               Logger logger) {
        log.info("Starting container with Docker image: {}", container.getDockerImageName());
        GenericContainer<?> updatedContainer = container
                .withStartupTimeout(properties.getTimeoutDuration())
                .withReuse(properties.isReuseContainer())
                .withLogConsumer(containerLogsConsumer(logger))
                .withImagePullPolicy(resolveImagePullPolicy(properties))
                .withEnv(properties.getEnv());

        if (!properties.getTmpFs().getMounts().isEmpty()) {
            Map<String, String> tmpFsMapping = properties.getTmpFs().getMounts().stream()
                    .collect(Collectors.toMap(TmpFsMount::getFolder, TmpFsMount::getOptions));
            updatedContainer.withTmpFs(tmpFsMapping);
        }

        for (CopyFileProperties fileToCopy : properties.getFilesToInclude()) {
            MountableFile mountableFile = MountableFile.forClasspathResource(fileToCopy.getClasspathResource());
            updatedContainer = updatedContainer.withCopyFileToContainer(mountableFile, fileToCopy.getContainerPath());
        }

        for (MountVolume mountVolume : properties.getMountVolumes()) {
            updatedContainer.addFileSystemBind(mountVolume.getHostPath(), mountVolume.getContainerPath(), mountVolume.getMode());
        }

        for (Capability capability : properties.getCapabilities()) {
            updatedContainer.withCreateContainerCmdModifier(cmd -> cmd.getHostConfig().withCapAdd(capability));
        }

        updatedContainer = properties.getCommand() != null ? updatedContainer.withCommand(properties.getCommand()) : updatedContainer;

        startAndLogTime(updatedContainer, logger);
        return updatedContainer;
    }

    private long startAndLogTime(GenericContainer<?> container, Logger logger) {
        Instant startTime = Instant.now();
        container.start();
        long startupTime = Duration.between(startTime, Instant.now()).toMillis() / 1000;

        String dockerImageName = container.getDockerImageName();
        String buildDate = getBuildDate(container, dockerImageName);
        // influxdb:1.4.3 build 2018-07-06T17:25:49+02:00 (2 years 11 months ago) startup time is 21 seconds
        if (startupTime < 10L) {
            logger.info("{} build {} startup time is {} seconds", dockerImageName, buildDate, startupTime);
        } else if (startupTime < 20L) {
            logger.warn("{} build {} startup time is {} seconds", dockerImageName, buildDate, startupTime);
        } else {
            logger.error("{} build {} startup time is {} seconds", dockerImageName, buildDate, startupTime);
        }
        return startupTime;
    }

    private String getBuildDate(GenericContainer<?> container, String dockerImageName) {
        String imageResponseCreated = null;
        try {
            InspectImageResponse inspectImageResponse = container.getDockerClient().inspectImageCmd(dockerImageName).exec();
            if (inspectImageResponse != null) {
                imageResponseCreated = inspectImageResponse.getCreated();
                return DateUtils.toDateAndTimeAgo(imageResponseCreated);
            } else {
                log.error("InspectImageResponse was null");
            }
        } catch (NotFoundException e) {
            log.error("Could not get InspectImageResponse", e);
        }
        return imageResponseCreated;
    }

    private static ImagePullPolicy resolveImagePullPolicy(CommonContainerProperties properties) {
        return properties.isUsePullAlwaysPolicy() ? PullPolicy.alwaysPull() : PullPolicy.defaultPolicy();
    }

    public static int getAvailableMappingPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot find available port for mapping: " + e.getMessage(), e);
        }
    }

    public static Consumer<OutputFrame> containerLogsConsumer(Logger log) {
        return (OutputFrame outputFrame) -> {
            switch (outputFrame.getType()) {
                case STDERR:
                    log.debug(outputFrame.getUtf8String());
                    break;
                case STDOUT:
                case END:
                    log.debug(outputFrame.getUtf8String());
                    break;
                default:
                    log.debug(outputFrame.getUtf8String());
                    break;
            }
        };
    }

    public static Container.ExecResult executeInContainer(ContainerState container, String... command) {
        try {
            return container.execInContainer(command);
        } catch (Exception e) {
            String format = String.format("Exception was thrown when executing: %s, for container: %s ", Arrays.toString(command), container.getContainerId());
            throw new IllegalStateException(format, e);
        }
    }

    public static Container.ExecResult executeAndCheckExitCode(ContainerState container, String... command) {
        try {
            Container.ExecResult execResult = container.execInContainer(command);
            log.debug("Executed command in container: {} with result: {}", container.getContainerId(), execResult);
            if (execResult.getExitCode() != 0) {
                throw new IllegalStateException("Failed to execute command. Execution result: " + execResult);
            }
            return execResult;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to execute command in container: " + container.getContainerId(), e);
        }
    }

}
