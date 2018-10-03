package com.playtika.test.common.properties;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class PackageProperties {

    Set<String> packages = new HashSet<>();
}
