package com.playtika.test.pubsub;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collection;
import java.util.Collections;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.google.pubsub")
public class PubsubProperties extends CommonContainerProperties {
    public static final String BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB = "embeddedGooglePubsub";
    // https://hub.docker.com/r/google/cloud-sdk
    // Only the full image "latest" (or just the version number) and the much smaller "emulators" contain all components needed for testing pubsub
    public static final String BASE_DOCKER_IMAGE = "google/cloud-sdk:351.0.0";
    private String dockerImage = BASE_DOCKER_IMAGE;
    private String host = "0.0.0.0";
    private int port = 8089;
    private String projectId = "my-project-id";
    private Collection<TopicAndSubscription> topicsAndSubscriptions = Collections.emptyList();
}
