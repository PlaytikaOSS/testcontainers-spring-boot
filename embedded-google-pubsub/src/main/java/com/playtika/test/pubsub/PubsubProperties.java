package com.playtika.test.pubsub;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collection;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.google.pubsub")
public class PubsubProperties extends CommonContainerProperties {
    public static final String BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB = "embeddedPubsub";
    private String dockerImage = "google/cloud-sdk:257.0.0";
    private String host = "0.0.0.0";
    private int port = 8089;
    private String projectId = "my-project-id";
    private Collection<TopicAndSubscription> topicsAndSubscriptions;
}
