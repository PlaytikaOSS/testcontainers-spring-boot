package com.playtika.test.mariadb;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("embedded.mariadb")
public class MariaDBProperties {
    static final String BEAN_NAME_EMBEDDED_MARIADB = "embeddedMariaDb";
    boolean enabled;
    String dockerImage = "mariadb:10.3.2";
    final String user = "user";
    final String password = "passw";
    final String database = "test_db";
    final int port = 3306;
}
