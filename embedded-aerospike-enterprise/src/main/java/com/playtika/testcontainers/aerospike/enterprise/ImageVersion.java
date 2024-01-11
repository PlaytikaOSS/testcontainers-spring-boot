package com.playtika.testcontainers.aerospike.enterprise;

import lombok.NonNull;
import lombok.Value;

import java.util.Comparator;

@Value
public class ImageVersion implements Comparable<ImageVersion> {

    int major;
    int minor;

    static ImageVersion parse(String version) {
        String[] parts = version.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid version: " + version);
        }
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            return new ImageVersion(major, minor);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid version: " + version, e);
        }
    }

    @Override
    public int compareTo(@NonNull ImageVersion o) {
        return Comparator.comparingInt(ImageVersion::getMajor)
                .thenComparingInt(ImageVersion::getMinor)
                .compare(this, o);
    }
}
