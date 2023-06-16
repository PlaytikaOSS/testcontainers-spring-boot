package com.playtika.testcontainer.redis;

import lombok.experimental.UtilityClass;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.playtika.testcontainer.redis.EmbeddedRedisBootstrapConfiguration.REDIS_NETWORK_ALIAS;

@UtilityClass
class EnvUtils {
    static Map<String, Object> registerRedisEnvironment(ConfigurableEnvironment environment, GenericContainer<?> redis,
                                                        RedisProperties properties, int port) {
        String host = redis.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.redis.port", port);
        map.put("embedded.redis.host", host);
        map.put("embedded.redis.password", properties.getPassword());
        map.put("embedded.redis.user", properties.getUser());
        map.put("embedded.redis.networkAlias", REDIS_NETWORK_ALIAS);
        MapPropertySource propertySource = new MapPropertySource("embeddedRedisInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        return map;
    }
}
