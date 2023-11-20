package com.playtika.testcontainer.redis;

import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import com.playtika.testcontainer.common.utils.TcpPortAvailableUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.playtika.testcontainer.common.utils.TcpPortAvailableUtils.PORT_RANGE_MIN;

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

    @Override
    public void afterPropertiesSet() {
        if (this.port == 0) {
            this.port = TcpPortAvailableUtils.findAvailableTcpPort(PORT_RANGE_MIN, 50000);
        }
    }

    // https://hub.docker.com/_/redis
    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "redis:7.2.3-alpine";
    }
}
