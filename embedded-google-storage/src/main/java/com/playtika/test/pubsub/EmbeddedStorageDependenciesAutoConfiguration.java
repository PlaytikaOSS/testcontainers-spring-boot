package com.playtika.test.pubsub;

import com.google.cloud.storage.Storage;
import com.playtika.test.common.spring.DependsOnPostProcessor;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.playtika.test.pubsub.StorageProperties.BEAN_NAME_EMBEDDED_GOOGLE_STORAGE;

@Configuration
@AutoConfigureOrder
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnClass(Storage.class)
@ConditionalOnProperty(name = "embedded.google.storage.enabled", matchIfMissing = true)
public class EmbeddedStorageDependenciesAutoConfiguration {
    @Bean
    public static BeanFactoryPostProcessor storageDependencyPostProcessor() {
        return new DependsOnPostProcessor(Storage.class, new String[]{BEAN_NAME_EMBEDDED_GOOGLE_STORAGE});
    }
}