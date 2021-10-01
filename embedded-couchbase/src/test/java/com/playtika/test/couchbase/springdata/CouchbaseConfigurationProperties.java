package com.playtika.test.couchbase.springdata;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "couchbase")
public class CouchbaseConfigurationProperties {

    private String hosts;
    private String bucket;
    private String user;
    private String password;

    private int bootstrapHttpDirectPort;
    private int bootstrapCarrierDirectPort;

}