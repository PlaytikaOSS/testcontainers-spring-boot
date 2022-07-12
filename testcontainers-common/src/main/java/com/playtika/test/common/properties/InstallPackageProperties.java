package com.playtika.test.common.properties;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;


/**
 * Instead use ToxiProxy.
 */
@Data
@Deprecated
public class InstallPackageProperties {

    boolean enabled = false;
    Set<String> packages = new HashSet<>();
}
