package com.playtika.test.redis;

import com.playtika.test.common.properties.InstallPackageProperties;
import com.playtika.test.common.utils.ApkPackageInstaller;
import com.playtika.test.common.utils.PackageInstaller;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;

import static com.playtika.test.redis.RedisProperties.BEAN_NAME_EMBEDDED_REDIS;

@AutoConfiguration
@ConditionalOnBean({RedisProperties.class})
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(value = "embedded.redis.enabled", matchIfMissing = true)
public class EmbeddedRedisTestOperationsAutoConfiguration {

    @Bean
    @ConfigurationProperties("embedded.redis.install")
    public InstallPackageProperties redisPackageProperties() {
        return new InstallPackageProperties();
    }

    @Bean
    public PackageInstaller redisPackageInstaller(
            InstallPackageProperties redisPackageProperties,
            @Qualifier(BEAN_NAME_EMBEDDED_REDIS) GenericContainer<?> redis
    ) {
        return new ApkPackageInstaller(redisPackageProperties, redis);
    }
}
