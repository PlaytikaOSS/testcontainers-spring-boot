package com.playtika.testcontainer.temporal;

import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.temporal")
public class TemporalProperties extends CommonContainerProperties {

    public static final String BEAN_NAME_EMBEDDED_TEMPORAL = "embeddedTemporal";
    public static final int INTERNAL_PORT = 7233;
    public static final int INTERNAL_UI_PORT = 8233;

    private boolean uiEnabled;

    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "temporalio/admin-tools:1.27";
    }
}
