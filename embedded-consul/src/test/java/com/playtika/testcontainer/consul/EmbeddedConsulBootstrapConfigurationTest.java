package com.playtika.testcontainer.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        properties = "embedded.consul.enabled=true",
        classes = TestConfiguration.class
)
public class EmbeddedConsulBootstrapConfigurationTest extends EmbeddedConsulBootstrapConfigurationBaseTest {

    @Test
    public void propertiesAvailable() {
        assertThat(environment.getProperty("embedded.consul.enabled"))
                .isEqualTo("true");
        assertThat(environment.getProperty("embedded.consul.configurationFile"))
                .isEqualTo(null);
        assertThat(environment.getProperty("embedded.consul.host"))
                .isEqualTo(consulContainer.getHost());
        assertThat(environment.getProperty("embedded.consul.port"))
                .isEqualTo(consulContainer.getFirstMappedPort().toString());
    }

    @Test
    public void shouldUpdateKey() {
        ConsulClient client = buildClient();

        Response<Boolean> booleanResponse = client.setKVValue("key", "val");
        assertThat(booleanResponse.getValue()).isEqualTo(true);
    }
}
