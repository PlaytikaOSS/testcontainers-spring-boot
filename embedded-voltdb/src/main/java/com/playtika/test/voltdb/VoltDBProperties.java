package com.playtika.test.voltdb;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.voltdb")
public class VoltDBProperties extends CommonContainerProperties {
    static final String BEAN_NAME_EMBEDDED_VOLTDB = "embeddedVoltDb";

    int port = 21212;

    // https://hub.docker.com/r/voltdb/voltdb-community
    @Override
    public String getDefaultDockerImage() {
        return "voltdb/voltdb-community:9.2.1";
    }
}
