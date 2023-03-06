package com.playtika.test.common.properties;

import com.github.dockerjava.api.model.Capability;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.springframework.validation.annotation.Validated;
import org.testcontainers.containers.BindMode;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Validated
@Data
public abstract class CommonContainerProperties {

    /**
     * Specify custom Docker image for the container.
     */
    private String dockerImage;

    /**
     * Overrides only version of the Docker image.
     */
    private String dockerImageVersion;
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

    /**
     * Tmpfs mount configuration.
     */
    @Valid
    private TmpFs tmpFs = new TmpFs();

    public Duration getTimeoutDuration() {
        return Duration.ofSeconds(waitTimeoutInSeconds);
    }

    /**
     * Specify default Docker image that is used by org.testcontainers:xyz module,
     * so that we can mark your custom image as a compatible with the default one.
     * <p>
     * For more details check {@link com.playtika.test.common.utils.ContainerUtils#getDockerImageName}.
     */
    public abstract String getDefaultDockerImage();

    /**
     * Copy a local file or directory from the classpath into the container.
     */
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @With
    @Validated
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
    @Validated
    public static class MountVolume {
        @NotBlank
        String hostPath;
        @NotBlank
        String containerPath;
        @NotNull
        BindMode mode = BindMode.READ_ONLY;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Validated
    public static class TmpFs {

        /**
         * A list of container directories which should be replaced by tmpfs mounts, and their corresponding mount options.
         */
        @Valid
        List<TmpFsMount> mounts = new ArrayList<>();

        @AllArgsConstructor
        @NoArgsConstructor
        @Data
        @Validated
        public static class TmpFsMount {

            /**
             * Folder that should be replaced by tmpfs mount.
             */
            @NotBlank
            String folder;

            /**
             * Mount options.
             */
            String options = "";
        }
    }
}
