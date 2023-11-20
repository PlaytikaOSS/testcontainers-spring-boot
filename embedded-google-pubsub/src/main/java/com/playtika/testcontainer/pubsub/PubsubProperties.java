package com.playtika.testcontainer.pubsub;

import com.playtika.testcontainer.common.properties.CommonContainerProperties;
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
    private String host = "0.0.0.0";
    private int port = 8089;
    private String projectId = "my-project-id";
    private Collection<TopicAndSubscription> topicsAndSubscriptions = Collections.emptyList();

    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "google/cloud-sdk:455.0.0";
    }
}
