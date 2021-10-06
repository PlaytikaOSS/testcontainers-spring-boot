package com.playtika.test.influxdb;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.influxdb")
public class InfluxDBProperties extends CommonContainerProperties {

    static final String EMBEDDED_INFLUX_DB = "embeddedInfluxDB";
    // https://hub.docker.com/_/influxdb
    String dockerImage = "influxdb:2.0-alpine";

    String adminUser = "admin";
    String adminPassword = "password";
    boolean enableHttpAuth = true;
    String user = "any";
    String password = "any";
    String host = "localhost";
    String database = "db";
    int port = 8086;
}
