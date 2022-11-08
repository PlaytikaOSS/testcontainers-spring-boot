package com.playtika.test.redis;

import com.github.dockerjava.api.model.Capability;
import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.SocketUtils;

import javax.annotation.PostConstruct;

import java.util.Arrays;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.redis")
public class RedisProperties extends CommonContainerProperties {
    public static final String BEAN_NAME_EMBEDDED_REDIS = "embeddedRedis";

    public String user = "root";
    public String password = "passw";
    public String host = "localhost";
    public int port = 0;
    public boolean requirepass = true;
    public boolean clustered = false;

    public RedisProperties() {
        this.setCapabilities(Arrays.asList(Capability.NET_ADMIN));
    }

    @PostConstruct
    public void init() {
        if (this.port == 0) {
            this.port = SocketUtils.findAvailableTcpPort(1025, 10000);
        }
    }

    // https://hub.docker.com/_/redis
    @Override
    public String getDefaultDockerImage() {
        return "redis:6.2-alpine";
    }
}
