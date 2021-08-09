package com.playtika.test.localstack;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.testcontainers.containers.localstack.LocalStackContainer;

import java.util.Collection;
import java.util.Collections;

@Accessors(chain = true)
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
    // https://hub.docker.com/r/localstack/localstack
    public String dockerImage = "localstack/localstack:0.12.16";
    public boolean useSsl = false;
}
