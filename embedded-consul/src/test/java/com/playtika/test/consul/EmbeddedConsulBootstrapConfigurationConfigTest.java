package com.playtika.test.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.OperationException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(
        properties = {
                "embedded.consul.enabled=true",
                "embedded.consul.configurationFile=consul.hcl"
        },
        classes = TestConfiguration.class
)
public class EmbeddedConsulBootstrapConfigurationConfigTest extends EmbeddedConsulBootstrapConfigurationBaseTest {

    @Test
    public void propertiesAvailable() {
        assertThat(environment.getProperty("embedded.consul.enabled"))
                .isEqualTo("true");
        assertThat(environment.getProperty("embedded.consul.host"))
                .isEqualTo(consulContainer.getHost());
        assertThat(environment.getProperty("embedded.consul.port"))
                .isEqualTo(consulContainer.getFirstMappedPort().toString());
        assertThat(environment.getProperty("embedded.consul.configurationFile"))
                .isEqualTo("consul.hcl");
    }

    @Test
    public void shouldUpdateKeyForbidden() {
        ConsulClient client = buildClient();

        // with the loaded config consul should require use of an access token,
        // which is not provided so 403 should be returned by consul
        OperationException ex = assertThrows(OperationException.class, () -> {
            client.setKVValue("key", "val");
        });
        assertThat(ex.getStatusCode()).isEqualTo(403);
    }
}
