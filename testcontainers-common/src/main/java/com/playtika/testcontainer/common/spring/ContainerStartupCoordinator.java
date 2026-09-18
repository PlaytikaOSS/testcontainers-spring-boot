package com.playtika.testcontainer.common.spring;

import com.playtika.testcontainer.common.properties.TestcontainersProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Coordinates starting several embedded containers concurrently instead of one after another.
 * When {@code embedded.containers.parallelStartup} is disabled (the default), {@link #schedule}
 * runs its task immediately, preserving today's sequential behaviour. When enabled, tasks
 * accumulate until {@link #flush()} runs them all concurrently and blocks until they're done.
 * {@link #flush()} is called from the {@code allContainers} bean, which every real infrastructure
 * bean (DataSource, ConnectionFactory, ...) already transitively depends on via
 * {@code DependsOnPostProcessor} - so by the time those beans are created, every scheduled
 * container is guaranteed to be started and its properties registered.
 */
@Slf4j
public class ContainerStartupCoordinator implements DisposableBean {

    private final boolean parallelStartupEnabled;
    private final Queue<Runnable> pendingTasks = new ConcurrentLinkedQueue<>();
    private volatile ExecutorService executor;

    public ContainerStartupCoordinator(TestcontainersProperties properties) {
        this.parallelStartupEnabled = properties.isParallelStartup();
    }

    public void schedule(Runnable startTask) {
        if (!parallelStartupEnabled) {
            startTask.run();
            return;
        }
        pendingTasks.add(startTask);
    }

    public void flush() {
        if (!parallelStartupEnabled) {
            return;
        }
        List<Runnable> batch = new ArrayList<>();
        Runnable task;
        while ((task = pendingTasks.poll()) != null) {
            batch.add(task);
        }
        if (batch.isEmpty()) {
            return;
        }

        log.info("Starting {} container(s) in parallel", batch.size());
        ExecutorService executorService = executor();
        List<CompletableFuture<Void>> futures = batch.stream()
                .map(t -> CompletableFuture.runAsync(t, executorService))
                .toList();

        List<Throwable> failures = new ArrayList<>();
        for (CompletableFuture<Void> future : futures) {
            try {
                future.join();
            } catch (CompletionException e) {
                failures.add(e.getCause() != null ? e.getCause() : e);
            }
        }
        if (!failures.isEmpty()) {
            IllegalStateException combined = new IllegalStateException(
                    failures.size() + " container(s) failed to start in parallel");
            failures.forEach(combined::addSuppressed);
            throw combined;
        }
    }

    private synchronized ExecutorService executor() {
        if (executor == null) {
            executor = Executors.newCachedThreadPool(new CustomizableThreadFactory("container-startup-"));
        }
        return executor;
    }

    @Override
    public void destroy() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}
