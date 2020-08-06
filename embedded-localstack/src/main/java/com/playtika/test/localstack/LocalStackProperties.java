package com.playtika.test.localstack;

import java.util.Collection;
import java.util.Collections;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.testcontainers.containers.localstack.LocalStackContainer;

import com.playtika.test.common.properties.CommonContainerProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.localstack")
public class LocalStackProperties extends CommonContainerProperties {
    public static final String BEAN_NAME_EMBEDDED_LOCALSTACK = "embeddedLocalstack";
    public Collection<LocalStackContainer.Service> services = Collections.emptyList();
    public int edgePort = 4566;
    public String defaultRegion = "us-east-1";
    public String hostname = "localhost";
    public String hostnameExternal = "localhost";
    public String dockerImage = "localstack/localstack:0.10.8";
    public boolean useSsl = false;
}
