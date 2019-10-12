package com.playtika.test.pubsub;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "embedded.google.pubsub.topicsandsubscriptions",ignoreUnknownFields = false)
public class TopicAndSubscription {
    private String topic;
    private String subscription;
}