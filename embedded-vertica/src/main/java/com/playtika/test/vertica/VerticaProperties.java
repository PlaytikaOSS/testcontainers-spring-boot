package com.playtika.test.vertica;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.vertica")
public class VerticaProperties extends CommonContainerProperties {
    static final String BEAN_NAME_EMBEDDED_VERTICA = "embeddedVertica";

    String host = "localhost";
    Integer port = 5433;
    String database = "VMart";
    String user = "dbadmin";
    String password = "";

    // https://hub.docker.com/r/vertica/vertica-ce
    // https://github.com/vertica/vertica-kubernetes
    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "vertica/vertica-ce:11.1.1-0";
    }
}
