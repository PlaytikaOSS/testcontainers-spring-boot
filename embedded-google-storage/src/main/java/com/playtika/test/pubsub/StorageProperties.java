package com.playtika.test.pubsub;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.google.storage")
public class StorageProperties extends CommonContainerProperties {

    public static final String BEAN_NAME_EMBEDDED_GOOGLE_STORAGE = "embeddedGoogleStorage";
    public static final String BASE_DOCKER_IMAGE = "sergseven/fake-gcs-server:v2";
    private String dockerImage = BASE_DOCKER_IMAGE;
    /** using http or https */
    private String scheme = "https";
    /** host to bind to */
    private String host = "0.0.0.0";
    /** port to bind to */
    private int port = 4443;
    /** project id for storage */
    private String projectId = "US-CENTRAL1";
    /** location for buckets */
    private String bucketLocation = "US-CENTRAL1";

    /** project ID containing the pubsub topic */
    private String eventPubsubProjectId = "";
    /** pubsub topic name to publish events on */
    private String eventPubsubTopic = "";
    /** if not empty, only objects having this prefix will generate trigger events */
    private String eventPrefix = "";
    /** comma separated list of events to publish on cloud function URl. Options are: finalize, delete, and metadataUpdate */
    private String eventList = "finalize";
    //    private Collection<TopicAndSubscription> topicsAndSubscriptions = Collections.emptyList();
}
