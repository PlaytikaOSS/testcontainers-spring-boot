package com.playtika.test.storage;

import com.google.cloud.NoCredentials;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.playtika.test.storage.StorageProperties.BucketProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.util.Collection;

@Slf4j
public class StorageResourcesGenerator implements InitializingBean {

    private final Storage storage;
    private final Collection<BucketProperties> buckets;


    StorageResourcesGenerator(String storageHostUrl, StorageProperties storageProperties) {
        this.storage = buildStorageService(storageHostUrl, storageProperties.getProjectId());
        this.buckets = storageProperties.getBuckets();
    }

    private Storage buildStorageService(String storageHostUrl, String projectId) {
        return StorageOptions.newBuilder()
            .setHost(storageHostUrl)
            .setProjectId(projectId)
            .setCredentials(NoCredentials.getInstance())
            .build()
            .getService();
    }

    @Override
    public void afterPropertiesSet() {
        log.info("Creating buckets.");
        buckets.forEach(this::createBucket);
        log.info("Creating buckets done.");
    }

    private void createBucket(BucketProperties bucket) {
        storage.create(BucketInfo.newBuilder(bucket.name).build());

        log.info("bucket {} created", bucket.name);
    }
}
