package com.playtika.test.aerospike;

import com.playtika.test.common.operations.NetworkTestOperations;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.Duration;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class AerospikeTestOperations {

    private final ExpiredDocumentsCleaner expiredDocumentsCleaner;
    private final NetworkTestOperations networkTestOperations;
    private final GenericContainer aerospikeContainer;

    /**
     * @deprecated instead use {@link NetworkTestOperations} directly.
     */
    @Deprecated
    public void addNetworkLatencyForResponses(java.time.Duration millis) {
        networkTestOperations.addNetworkLatencyForResponses(millis);
    }

    /**
     * @deprecated instead use {@link NetworkTestOperations} directly.
     */
    @Deprecated
    public void removeNetworkLatencyForResponses() {
        networkTestOperations.removeNetworkLatencyForResponses();
    }

    public void addDuration(Duration duration) {
        timeTravel(DateTime.now().plus(duration).plusMinutes(1));
    }

    public void timeTravelTo(DateTime futureTime) {
        DateTime now = DateTime.now();
        if (futureTime.isBeforeNow()) {
            throw new IllegalArgumentException("Time should be in future. Now is: " + now + " time is:" + futureTime);
        } else {
            timeTravel(futureTime);
        }
    }

    public void rollbackTime() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    private void timeTravel(DateTime newNow) {
        DateTimeUtils.setCurrentMillisFixed(newNow.getMillis());
        expiredDocumentsCleaner.cleanExpiredDocumentsBefore(newNow.getMillis());
    }

    /**
     * More at https://www.aerospike.com/docs/guide/scan.html
     *
     * @return performed scans on aerospike server instance.
     */
    @SneakyThrows
    public List<ScanJob> getScans() {
        Container.ExecResult execResult = aerospikeContainer.execInContainer("asinfo", "-v", "scan-list");
        String stdout = execResult.getStdout();
        return getScanJobs(stdout);
    }

    private List<ScanJob> getScanJobs(String stdout) {
        if (StringUtils.isBlank(stdout)) {
            return Collections.emptyList();
        }
        return Arrays.stream(stdout.replaceAll("\n", "").split(";"))
                .map(this::parseToObScanJobObject)
                .collect(Collectors.toList());
    }

    private ScanJob parseToObScanJobObject(String job) {
        String[] pairs = job.split(":");
        Map<String, String> pairsMap = Arrays.stream(pairs)
                .map(pair -> {
                    String[] kv = pair.split("=");
                    return new AbstractMap.SimpleEntry<>(kv[0], kv[1]);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return ScanJob.builder()
                .module(pairsMap.get("module"))
                .set(pairsMap.get("set"))
                .udfFunction(pairsMap.get("udf-function"))
                .status(pairsMap.get("status"))
                .trid(pairsMap.get("trid"))
                .namespace(pairsMap.get("ns"))
                .build();
    }

    @SneakyThrows
    public void killAllScans() {
        Container.ExecResult execResult = aerospikeContainer.execInContainer("asinfo", "-v", "scan-abort-all:");
        assertThat(execResult.getStdout())
                .as("Scan jobs killed")
                .contains("OK");
    }

    public void assertNoScans() {
        assertNoScans(scanJob -> true);
    }

    public void assertNoScans(Predicate<ScanJob> scanJobPredicate) {
        List<ScanJob> scanJobs = getScans().stream()
                .filter(scanJobPredicate)
                .collect(Collectors.toList());
        assertThat(scanJobs)
                .as("Scan jobs")
                .isEmpty();
    }

    public void assertNoScansForSet(String setName) {
        assertNoScans(job -> setName.equals(job.set));
    }

    @Value
    @Builder
    public static class ScanJob {

        String module;
        String set;
        String udfFunction;
        String status;
        String trid;
        String namespace;
    }

}
