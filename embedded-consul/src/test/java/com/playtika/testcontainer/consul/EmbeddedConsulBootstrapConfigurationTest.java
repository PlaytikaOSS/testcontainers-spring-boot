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
        assertThat(consulEnabled)
                .isEqualTo("true");
        assertThat(consulConfigurationFile)
                .isEqualTo("");
        assertThat(consulHost)
                .isEqualTo(consulContainer.getHost());
        assertThat(consulPort)
                .isEqualTo(consulContainer.getFirstMappedPort().toString());
    }

    @Test
    public void shouldUpdateKey() {
        ConsulClient client = buildClient();

        Response<Boolean> booleanResponse = client.setKVValue("key", "val");
        assertThat(booleanResponse.getValue()).isEqualTo(true);
    }
}
