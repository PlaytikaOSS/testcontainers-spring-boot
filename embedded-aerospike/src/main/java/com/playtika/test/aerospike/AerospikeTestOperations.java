/*
* The MIT License (MIT)
*
* Copyright (c) 2019 Playtika
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
* SOFTWARE.
 */
package com.playtika.test.aerospike;

import com.playtika.test.common.operations.NetworkTestOperations;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.org.apache.commons.lang.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
        timeTravel(LocalDateTime.now().plus(duration).plusMinutes(1));
    }

    public void timeTravelTo(LocalDateTime futureTime) {
        LocalDateTime now = LocalDateTime.now();
        if (futureTime.isBefore(now)) {
            throw new IllegalArgumentException("Time should be in future. Now is: " + now + " time is:" + futureTime);
        } else {
            timeTravel(futureTime);
        }
    }

    public void rollbackTime() {
        //DateTimeUtils.setCurrentMillisSystem();
    }

    private void timeTravel(LocalDateTime newNow) {
        //DateTimeUtils.setCurrentMillisFixed(newNow.toInstant(ZoneOffset.systemDefault()).toEpochMilli());
        expiredDocumentsCleaner.cleanExpiredDocumentsBefore(newNow.toInstant(ZoneOffset.UTC));
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
