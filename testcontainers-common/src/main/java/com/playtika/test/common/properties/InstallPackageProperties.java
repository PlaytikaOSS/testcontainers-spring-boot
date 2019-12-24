package com.playtika.test.common.properties;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class InstallPackageProperties {

    boolean enabled = false;
    Set<String> packages = new HashSet<>();
}
