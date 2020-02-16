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
    String dockerImage = "influxdb:1.5-alpine";

    private String adminUser = "admin";
    private String adminPassword = "admin";
    private boolean enableHttpAuth = true;
    private String user = "user";
    private String password = "password";
    private String host = "localhost";
    private String database = "db";
    private int port = 8086;

}
