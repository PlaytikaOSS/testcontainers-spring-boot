package com.playtika.testcontainer.mongodb;

import com.github.dockerjava.api.model.Capability;
import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.mongodb")
public class MongodbProperties extends CommonContainerProperties {

    static final String BEAN_NAME_EMBEDDED_MONGODB = "embeddedMongodb";

    private String host = "localhost";
    /**
     * The container internal port. Will be overwritten with mapped port.
     */
    private int port = 27017;
    private String username;
    private String password;
    private String database = "test";
    private String[] checkCommand = new String[]{"mongosh", "admin", "--eval", "\"db['system.version'].find()\""};

    public MongodbProperties() {
        this.setCapabilities(Arrays.asList(Capability.NET_ADMIN));
    }

    // https://hub.docker.com/_/mongo
    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "mongo:6.0.9-jammy";
    }
}
