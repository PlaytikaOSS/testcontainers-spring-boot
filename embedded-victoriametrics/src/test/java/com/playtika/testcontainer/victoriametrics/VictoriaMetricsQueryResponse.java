package com.playtika.testcontainer.victoriametrics;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VictoriaMetricsQueryResponse(String status) {
}