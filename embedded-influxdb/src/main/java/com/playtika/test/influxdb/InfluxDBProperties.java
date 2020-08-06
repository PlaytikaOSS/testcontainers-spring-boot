package com.playtika.test.influxdb;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import com.playtika.test.common.properties.CommonContainerProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.influxdb")
public class InfluxDBProperties extends CommonContainerProperties {

    static final String EMBEDDED_INFLUX_DB = "embeddedInfluxDB";
    String dockerImage = "influxdb:1.4.3";

    String adminUser = "admin";
    String adminPassword = "password";
    boolean enableHttpAuth = true;
    String user = "any";
    String password = "any";
    String host = "localhost";
    String database = "db";
    int port = 8086;
}
