package com.playtika.test.mockserver;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.mockserver")
public class MockServerProperties extends CommonContainerProperties {
    static final String EMBEDDED_MOCK_SERVER = "embeddedMockServer";

    int port = 1080;

    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "mockserver/mockserver:5.15.0";
    }
}
