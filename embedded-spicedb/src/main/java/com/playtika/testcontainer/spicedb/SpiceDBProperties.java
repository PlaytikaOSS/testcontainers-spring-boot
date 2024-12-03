package com.playtika.testcontainer.spicedb;

import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.spicedb")
public class SpiceDBProperties extends CommonContainerProperties {
    static final String BEAN_NAME_EMBEDDED_SPICEDB = "embeddedSpiceDB";
    static final String BEAN_NAME_EMBEDDED_SPICEDB_TOXI_PROXY = "embeddedSpiceDbToxiProxy";

    int port = 50051;
    String presharedKey = "somerandomkeyhere";

    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "authzed/spicedb:v1.38.1";
    }
}
