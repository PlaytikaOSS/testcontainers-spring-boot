package com.playtika.testcontainer.victoriametrics;

public record VictoriaMetricsHttpResponse(int statusCode, VictoriaMetricsQueryResponse body) {
}