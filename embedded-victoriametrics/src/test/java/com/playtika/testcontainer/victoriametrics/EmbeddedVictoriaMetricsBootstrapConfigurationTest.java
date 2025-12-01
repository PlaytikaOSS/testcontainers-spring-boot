package com.playtika.testcontainer.victoriametrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EmbeddedVictoriaMetricsBootstrapConfigurationTest extends BaseEmbeddedVictoriaMetricsTest {

    @Test
    void shouldHaveMetrics() throws Exception {
        VictoriaMetricsHttpResponse actual = queryUp(victoriaMetricsHost, victoriaMetricsPort);
        VictoriaMetricsHttpResponse expected = new VictoriaMetricsHttpResponse(200, new VictoriaMetricsQueryResponse("success"));
        assertThat(actual).isEqualTo(expected);
    }
}
