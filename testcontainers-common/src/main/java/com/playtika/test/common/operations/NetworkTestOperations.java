package com.playtika.test.common.operations;

import com.playtika.test.common.utils.ThrowingRunnable;

import java.time.Duration;
import java.util.concurrent.Callable;

public interface NetworkTestOperations {

    void addNetworkLatencyForResponses(Duration delay);

    void removeNetworkLatencyForResponses();

    void withNetworkLatency(Duration delay, ThrowingRunnable runnable);

    <T> T withNetworkLatency(Duration delay, Callable<T> callable);
}
