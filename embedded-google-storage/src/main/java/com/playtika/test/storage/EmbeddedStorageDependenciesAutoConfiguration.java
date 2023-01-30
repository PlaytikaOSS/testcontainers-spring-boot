package com.playtika.test.storage;

import com.google.cloud.storage.Storage;
import com.playtika.test.common.spring.DependsOnPostProcessor;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import static com.playtika.test.storage.StorageProperties.BEAN_NAME_EMBEDDED_GOOGLE_STORAGE_SERVER;

@AutoConfiguration
@AutoConfigureOrder
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnClass(Storage.class)
@ConditionalOnProperty(name = "embedded.google.storage.enabled", matchIfMissing = true)
public class EmbeddedStorageDependenciesAutoConfiguration {

    @Bean
    static BeanFactoryPostProcessor storageDependencyPostProcessor() {
        return new DependsOnPostProcessor(Storage.class, new String[]{BEAN_NAME_EMBEDDED_GOOGLE_STORAGE_SERVER});
    }
}