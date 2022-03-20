package com.playtika.test.pubsub;

import lombok.Data;

@Data
public class TopicAndSubscription {
    private String topic;
    private String subscription;
    private DeadLetter deadLetter;

    @Data
    public static class DeadLetter {
        private String topic;
        private int maxAttempts;
    }
}
