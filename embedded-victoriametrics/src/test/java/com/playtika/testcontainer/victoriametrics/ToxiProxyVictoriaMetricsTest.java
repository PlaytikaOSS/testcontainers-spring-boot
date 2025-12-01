package com.playtika.testcontainer.victoriametrics;

import com.playtika.testcontainer.toxiproxy.ToxiproxyClientProxy;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.http.HttpTimeoutException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ToxiProxyVictoriaMetricsTest extends BaseEmbeddedVictoriaMetricsTest {

    @Autowired
    private ToxiproxyClientProxy victoriaMetricsContainerProxy;

    @Test
    void shouldAddLatency() throws Exception {
        victoriaMetricsContainerProxy.toxics()
                .latency("latency", ToxicDirection.DOWNSTREAM, 1_100)
                .setJitter(100);

        assertThatThrownBy(() -> queryUp(victoriaMetricsToxiProxyHost, victoriaMetricsToxiProxyPort, Duration.ofMillis(200)))
                .isInstanceOf(HttpTimeoutException.class);

        victoriaMetricsContainerProxy.toxics().get("latency").remove();

        VictoriaMetricsHttpResponse actual = queryUp(victoriaMetricsToxiProxyHost, victoriaMetricsToxiProxyPort);
        VictoriaMetricsHttpResponse expected = new VictoriaMetricsHttpResponse(200, new VictoriaMetricsQueryResponse("success"));
        assertThat(actual).isEqualTo(expected);
    }
}
