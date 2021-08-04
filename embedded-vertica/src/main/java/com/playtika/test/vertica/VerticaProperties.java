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

    // https://hub.docker.com/r/jbfavre/vertica 9.2
    // https://github.com/vertica/vertica-kubernetes
    // https://hub.docker.com/r/verticadocker/vertica-k8s
    String dockerImage = "jbfavre/vertica:9.2.0-7_debian-8";

    String host = "localhost";
    Integer port = 5433;
    String database = "docker";
    String user = "dbadmin";
    String password = "";
}
