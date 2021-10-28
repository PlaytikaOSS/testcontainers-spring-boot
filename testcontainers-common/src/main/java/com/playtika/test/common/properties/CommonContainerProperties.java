package com.playtika.test.common.properties;

import com.github.dockerjava.api.model.Capability;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.springframework.validation.annotation.Validated;
import org.testcontainers.containers.BindMode;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Validated
@Data
public class CommonContainerProperties {

    /**
     * Maximum time in seconds until embedded container should have started.
     */
    private long waitTimeoutInSeconds = 60;
    /**
     * Enable embedded container.
     */
    private boolean enabled = true;
    /**
     * Reuse embedded container.
     */
    private boolean reuseContainer = false;
    /**
     * Whether to pull a docker image each time.
     */
    private boolean usePullAlwaysPolicy = false;

    /**
     * Container startup command.
     */
    private String[] command;

    /**
     * Set environment variables for the container.
     */
    private Map<String, String> env = new HashMap<>();
    /**
     * Files/directories that should be copied to the container.
     */
    @Valid
    private List<CopyFileProperties> filesToInclude = new ArrayList<>();
    /**
     * Files/directories that should be mounted as container volumes.
     */
    @Valid
    private List<MountVolume> mountVolumes = new ArrayList<>();

    /**
     * The Linux capabilities that should be enabled.
     */
    private List<Capability> capabilities = new ArrayList<>();

    public Duration getTimeoutDuration() {
        return Duration.ofSeconds(waitTimeoutInSeconds);
    }

    /**
     * Copy a local file or directory from the classpath into the container.
     */
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @With
    public static class CopyFileProperties {
        @NotBlank
        String classpathResource;
        @NotBlank
        String containerPath;
    }

    /**
     * Mount a local file or directory from the host as a container volume with READ_ONLY or READ_WRITE access mode.
     */
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @With
    public static class MountVolume {
        @NotBlank
        String hostPath;
        @NotBlank
        String containerPath;
        @NotNull
        BindMode mode = BindMode.READ_ONLY;
    }
}
