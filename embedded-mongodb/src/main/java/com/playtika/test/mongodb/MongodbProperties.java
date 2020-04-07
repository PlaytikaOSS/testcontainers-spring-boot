package com.playtika.test.mongodb;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.mongodb")
public class MongodbProperties extends CommonContainerProperties {

    static final String BEAN_NAME_EMBEDDED_MONGODB = "embeddedMongodb";

    private String dockerImage = "mongo:4.2.0-bionic";
    private String host = "localhost";
    private int port = 27017;
    private String username;
    private String password;
    private String database = "test";

}
