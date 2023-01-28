package com.playtika.test.redis;

import com.github.dockerjava.api.model.Capability;
import com.playtika.test.common.properties.CommonContainerProperties;
import com.playtika.test.common.utils.TcpPortAvailableUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;

import static com.playtika.test.common.utils.TcpPortAvailableUtils.PORT_RANGE_MIN;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.redis")
public class RedisProperties extends CommonContainerProperties implements InitializingBean {
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

    @Override
    public void afterPropertiesSet() {
        if (this.port == 0) {
            this.port = TcpPortAvailableUtils.findAvailableTcpPort(PORT_RANGE_MIN, 50000);
        }
    }

    // https://hub.docker.com/_/redis
    @Override
    public String getDefaultDockerImage() {
        return "redis:6.2-alpine";
    }
}
