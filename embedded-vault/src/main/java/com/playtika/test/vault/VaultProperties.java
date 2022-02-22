package com.playtika.test.vault;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.vault")
public class VaultProperties extends CommonContainerProperties {

    static final String BEAN_NAME_EMBEDDED_VAULT = "embeddedVault";

    private String host = "localhost";
    /**
     * The container internal port. Will be overwritten with mapped port.
     */
    private int port = 8200;
    private String token = "00000000-0000-0000-0000-000000000000";
    private String path = "secret/application";
    private final Map<String, String> secrets = new HashMap<>();

    // https://hub.docker.com/_/vault
    @Override
    public String getDefaultDockerImage() {
        return "vault:1.8.1";
    }
}
