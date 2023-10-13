package com.playtika.testcontainer.localstack;

import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.testcontainers.containers.localstack.LocalStackContainer;

import java.util.Collection;
import java.util.Collections;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.localstack")
public class LocalStackProperties extends CommonContainerProperties {
    public static final String BEAN_NAME_EMBEDDED_LOCALSTACK = "embeddedLocalstack";
    public Collection<LocalStackContainer.Service> services = Collections.emptyList();
    public int edgePort = 4566;
    public String hostname = "127.0.0.1";
    public String hostnameExternal = "127.0.0.1";

    // https://hub.docker.com/r/localstack/localstack
    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "localstack/localstack:2.3.2";
    }
}
