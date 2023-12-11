package com.playtika.testcontainer.redis;

import com.playtika.testcontainer.common.properties.InstallPackageProperties;
import com.playtika.testcontainer.common.utils.ApkPackageInstaller;
import com.playtika.testcontainer.common.utils.PackageInstaller;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;

import static com.playtika.testcontainer.redis.RedisProperties.BEAN_NAME_EMBEDDED_REDIS;
import static com.playtika.testcontainer.redis.RedisProperties.BEAN_NAME_EMBEDDED_REDIS_PACKAGE_PROPERTIES;

@AutoConfiguration
@ConditionalOnBean({RedisProperties.class})
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(value = "embedded.redis.enabled", matchIfMissing = true)
public class EmbeddedRedisTestOperationsAutoConfiguration {

    @Bean(BEAN_NAME_EMBEDDED_REDIS_PACKAGE_PROPERTIES)
    @ConfigurationProperties("embedded.redis.install")
    public InstallPackageProperties redisPackageProperties() {
        return new InstallPackageProperties();
    }

    @Bean
    public PackageInstaller redisPackageInstaller(
            @Qualifier(BEAN_NAME_EMBEDDED_REDIS_PACKAGE_PROPERTIES) InstallPackageProperties redisPackageProperties,
            @Qualifier(BEAN_NAME_EMBEDDED_REDIS) GenericContainer<?> redis
    ) {
        return new ApkPackageInstaller(redisPackageProperties, redis);
    }
}
