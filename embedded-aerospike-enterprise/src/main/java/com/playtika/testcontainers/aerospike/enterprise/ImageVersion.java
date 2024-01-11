package com.playtika.testcontainers.aerospike.enterprise;

import lombok.NonNull;

import java.util.Comparator;

record ImageVersion (int major, int minor) implements Comparable<ImageVersion> {

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
        return Comparator.comparingInt(ImageVersion::major)
                .thenComparingInt(ImageVersion::minor)
                .compare(this, o);
    }
}
