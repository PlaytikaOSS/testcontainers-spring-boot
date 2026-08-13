package com.playtika.testcontainer.common.spring;

import com.playtika.testcontainer.common.properties.TestcontainersProperties;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContainerStartupCoordinatorTest {

    private ContainerStartupCoordinator coordinator(boolean parallelStartup) {
        TestcontainersProperties properties = new TestcontainersProperties();
        properties.setParallelStartup(parallelStartup);
        return new ContainerStartupCoordinator(properties);
    }

    @Test
    void shouldRunTaskImmediatelyWhenParallelStartupDisabled() {
        ContainerStartupCoordinator coordinator = coordinator(false);
        AtomicInteger executions = new AtomicInteger();

        coordinator.schedule(executions::incrementAndGet);

        assertThat(executions.get()).isEqualTo(1);
    }

    @Test
    void shouldRunScheduledTasksConcurrently() throws Exception {
        ContainerStartupCoordinator coordinator = coordinator(true);
        // trips only if both tasks reach it at roughly the same time - proves flush() runs
        // scheduled tasks concurrently rather than one after another
        CyclicBarrier barrier = new CyclicBarrier(2);

        coordinator.schedule(() -> awaitBarrier(barrier));
        coordinator.schedule(() -> awaitBarrier(barrier));

        coordinator.flush();
    }

    @Test
    void shouldAggregateFailuresFromAllScheduledTasks() {
        ContainerStartupCoordinator coordinator = coordinator(true);
        RuntimeException first = new RuntimeException("first container failed");
        RuntimeException second = new RuntimeException("second container failed");

        coordinator.schedule(() -> {
            throw first;
        });
        coordinator.schedule(() -> {
            throw second;
        });

        assertThatThrownBy(coordinator::flush)
                .isInstanceOf(IllegalStateException.class)
                .satisfies(e -> assertThat(e.getSuppressed()).containsExactlyInAnyOrder(first, second));
    }

    @Test
    void shouldNotRerunAlreadyCompletedTasksOnRepeatedFlush() {
        ContainerStartupCoordinator coordinator = coordinator(true);
        AtomicInteger executions = new AtomicInteger();

        coordinator.schedule(executions::incrementAndGet);
        coordinator.flush();
        coordinator.flush();

        assertThat(executions.get()).isEqualTo(1);
    }

    @Test
    void shouldDoNothingWhenNothingIsScheduled() {
        ContainerStartupCoordinator coordinator = coordinator(true);

        coordinator.flush();
    }

    private static void awaitBarrier(CyclicBarrier barrier) {
        try {
            barrier.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
