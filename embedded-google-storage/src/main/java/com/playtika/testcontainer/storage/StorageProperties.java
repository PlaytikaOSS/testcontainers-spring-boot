package com.playtika.testcontainer.storage;

import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collection;

import static java.util.Collections.emptyList;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.google.storage")
public class StorageProperties extends CommonContainerProperties {

    public static final String BEAN_NAME_EMBEDDED_GOOGLE_STORAGE_SERVER = "embeddedGoogleStorageServer";
    /** default port exposed by the fake-gcs-server */
    static final int PORT = 4443;
    /** project id for storage */
    private String projectId = "my-project-id";
    /** location for buckets */
    private String bucketLocation = "US-CENTRAL1";
    /** list with predefined buckets to be created */
    private Collection<BucketProperties> buckets = emptyList();

    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "fsouza/fake-gcs-server:1.47.5";
    }

    @Data
    static class BucketProperties {
        String name;
    }
}
