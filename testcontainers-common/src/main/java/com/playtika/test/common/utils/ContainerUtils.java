/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Playtika
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

import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.playtika.test.common.properties.CommonContainerProperties;
import com.playtika.test.common.properties.CommonContainerProperties.CopyFileProperties;
import com.playtika.test.common.properties.CommonContainerProperties.MountVolume;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.images.ImagePullPolicy;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.function.Consumer;

import static com.playtika.test.common.utils.DateUtils.NO_DATE_REPRODUCIBLE_BUILD;

@Slf4j
@UtilityClass
public class ContainerUtils {

    public static final Duration DEFAULT_CONTAINER_WAIT_DURATION = Duration.ofSeconds(60);

    /**
     * Configure and start container (don't log build date, startup time and container messages)
     */
    public static GenericContainer<?> configureCommonsAndStart(GenericContainer<?> container,
                                                               CommonContainerProperties properties) {
        return configureCommonsAndStart(container, properties, null, null);
    }

    /**
     * Configure and start container
     * @param logger log build date and startup time, and container messages with default {@link #containerLogsConsumer(org.slf4j.Logger)} (don't log if {@code null})
     */
    public static GenericContainer<?> configureCommonsAndStart(GenericContainer<?> container,
                                                               CommonContainerProperties properties,
                                                               Logger logger) {
        return configureCommonsAndStart(container, properties, logger, null);
    }

    /**
     * Configure and start container (don't log if {@code logger} and {@code logConsumer} are {@code null})
     * @param logger log build date, startup time and container messages
     * @param logConsumer log container messages with custom consumer (if {@code null} use {@code logger} with default {@link #containerLogsConsumer})
     */
    public static GenericContainer<?> configureCommonsAndStart(GenericContainer<?> container,
                                                               CommonContainerProperties properties,
                                                               Logger logger,
                                                               Consumer<OutputFrame> logConsumer) {
        GenericContainer<?> updatedContainer = container
                .withStartupTimeout(properties.getTimeoutDuration())
                .withReuse(properties.isReuseContainer())
                .withImagePullPolicy(resolveImagePullPolicy(properties))
                .withEnv(properties.getEnv());
        if (logConsumer == null && logger != null) {
            logConsumer = containerLogsConsumer(logger);
        }
        if (logConsumer != null) {
            updatedContainer.withLogConsumer(logConsumer);
        }

        for (CopyFileProperties fileToCopy : properties.getFilesToInclude()) {
            MountableFile mountableFile = MountableFile.forClasspathResource(fileToCopy.getClasspathResource());
            updatedContainer = updatedContainer.withCopyFileToContainer(mountableFile, fileToCopy.getContainerPath());
        }

        for (MountVolume mountVolume : properties.getMountVolumes()) {
            updatedContainer.addFileSystemBind(mountVolume.getHostPath(), mountVolume.getContainerPath(), mountVolume.getMode());
        }

        updatedContainer = properties.getCommand() != null ? updatedContainer.withCommand(properties.getCommand()) : updatedContainer;

        startAndLogTime(updatedContainer, logger, properties);
        return updatedContainer;
    }

    private long startAndLogTime(GenericContainer<?> container, Logger logger, CommonContainerProperties properties) {
        Instant startTime = Instant.now();
        container.start();
        long startupTime = Duration.between(startTime, Instant.now()).toMillis() / 1000;
        if (logger == null) {
            return startupTime;
        }

        String dockerImageName = container.getDockerImageName();
        String buildDate = getBuildDate(container, dockerImageName, properties);
        if (!buildDate.isEmpty()) {
            buildDate = " " + buildDate;
        }
        // influxdb:1.4.3 build 2019-07-06T17:25:49+02:00 (1 years 11 months ago) startup time is 21 seconds
        // without time:  build 2018-01-01 (3 years ago) startup time is 21 seconds
        // EPOCH (1970):  (no date / reproducible build) startup time is 21 seconds
        if (startupTime < 10L) {
            logger.info("{}{} startup time is {} seconds", dockerImageName, buildDate, startupTime);
        } else if (startupTime < 20L) {
            logger.warn("{}{} startup time is {} seconds", dockerImageName, buildDate, startupTime);
        } else {
            logger.error("{}{} startup time is {} seconds", dockerImageName, buildDate, startupTime);
        }
        return startupTime;
    }

    /**
     * @param properties Checked if logging date and time is enabled
     * @return Container build date and time, empty string on error or if disabled
     */
    public static String getBuildDate(GenericContainer<?> container, String dockerImageName, CommonContainerProperties properties) {
        if (!properties.isLogBuildDate()) {
            return "";
        }
        try {
            InspectImageResponse inspectImageResponse = container.getDockerClient().inspectImageCmd(dockerImageName).exec();
            if (inspectImageResponse != null) {
                String imageResponseCreated = inspectImageResponse.getCreated();
                String dateAndTimeAgo = DateUtils.toDateAndTimeAgo(imageResponseCreated, properties.isLogBuildTime());
                return NO_DATE_REPRODUCIBLE_BUILD.equals(dateAndTimeAgo) ? dateAndTimeAgo : "build " + dateAndTimeAgo;
            } else {
                log.error("InspectImageResponse was null");
            }
        } catch (NotFoundException e) {
            log.error("Could not get InspectImageResponse", e);
        }
        return "";
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

    /**
     * Default logs consumer (trim messages, log with debug level, don't log last message if empty)
     * @return Consumer for {@link GenericContainer#withLogConsumer(java.util.function.Consumer)}
     */
    public static Consumer<OutputFrame> containerLogsConsumer(Logger logger) {
        return (OutputFrame outputFrame) -> {
            String trimmed = outputFrame.getUtf8String().trim();
            switch (outputFrame.getType()) {
                case STDERR:
                    logger.error(trimmed);
                    break;
                case END:
                    if (!trimmed.isEmpty()) {
                        logger.info(trimmed);
                    }
                    break;
                default:
                    logger.debug(trimmed);
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
