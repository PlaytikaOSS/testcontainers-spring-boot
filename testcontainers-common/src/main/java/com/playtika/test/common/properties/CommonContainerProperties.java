/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Playtika
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
package com.playtika.test.common.properties;

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
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

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
    private String[] command;
    private Map<String, String> env = emptyMap();
    @Valid
    private List<CopyFileProperties> filesToInclude = emptyList();
    @Valid
    private List<MountVolume> mountVolumes = emptyList();

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
