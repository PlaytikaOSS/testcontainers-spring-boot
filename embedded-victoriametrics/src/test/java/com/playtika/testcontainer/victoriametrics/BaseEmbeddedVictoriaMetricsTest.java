package com.playtika.testcontainer.victoriametrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = BaseEmbeddedVictoriaMetricsTest.TestConfiguration.class,
        properties = "embedded.toxiproxy.proxies.victoriametrics.enabled=true")
public class BaseEmbeddedVictoriaMetricsTest {

    @Value("${embedded.victoriametrics.host}")
    protected String victoriaMetricsHost;
    @Value("${embedded.victoriametrics.port}")
    protected int victoriaMetricsPort;
    @Value("${embedded.victoriametrics.toxiproxy.host}")
    protected String victoriaMetricsToxiProxyHost;
    @Value("${embedded.victoriametrics.toxiproxy.port}")
    protected int victoriaMetricsToxiProxyPort;
    @Autowired
    protected ConfigurableListableBeanFactory beanFactory;

    protected final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }

    protected VictoriaMetricsHttpResponse queryUp(String host, int port) throws IOException, InterruptedException {
        return queryUp(host, port, null);
    }

    protected VictoriaMetricsHttpResponse queryUp(String host, int port, Duration timeout) throws IOException, InterruptedException {
        URI uri = UriComponentsBuilder.newInstance()
                .scheme("http")
                .host(host)
                .port(port)
                .path("/api/v1/query")
                .queryParam("query", "up")
                .build(true)
                .toUri();

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri).GET();
        if (timeout != null) {
            requestBuilder.timeout(timeout);
        }

        HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        VictoriaMetricsQueryResponse body = objectMapper.readValue(response.body(), VictoriaMetricsQueryResponse.class);
        return new VictoriaMetricsHttpResponse(response.statusCode(), body);
    }
}
