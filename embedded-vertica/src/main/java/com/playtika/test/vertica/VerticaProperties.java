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

    String dockerImage = "jbfavre/vertica:latest";

    String host = "localhost";
    Integer port = 5433;
    String database = "docker";
    String user = "dbadmin";
    String password = "";
}
