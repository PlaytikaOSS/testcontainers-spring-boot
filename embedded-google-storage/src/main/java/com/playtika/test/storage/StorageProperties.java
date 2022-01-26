package com.playtika.test.storage;

import com.playtika.test.common.properties.CommonContainerProperties;
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
    public static final String BASE_DOCKER_IMAGE = "sergseven/fake-gcs-server:v3";
    /** default port exposed by the fake-gcs-server */
    static final int PORT = 4443;
    private String dockerImage = BASE_DOCKER_IMAGE;
    /** project id for storage */
    private String projectId = "my-project-id";
    /** location for buckets */
    private String bucketLocation = "US-CENTRAL1";
    /** list with predefined buckets to be created */
    private Collection<String> buckets = emptyList();
}
