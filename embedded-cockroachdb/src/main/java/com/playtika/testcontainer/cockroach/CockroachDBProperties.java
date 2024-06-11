package com.playtika.testcontainer.cockroach;

import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.cockroach")
public class CockroachDBProperties extends CommonContainerProperties {
    static final String BEAN_NAME_EMBEDDED_COCKROACHDB = "embeddedCockroachDb";

    int port = 26257;

    String initScriptPath;

    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "cockroachdb/cockroach:v23.2.5";
    }
}
