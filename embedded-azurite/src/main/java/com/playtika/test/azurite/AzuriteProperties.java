package com.playtika.test.azurite;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.azurite")
public class AzuriteProperties extends CommonContainerProperties {

    static final String AZURITE_BEAN_NAME = "azurite";

    /**
     * Using the latest version on purpose here, because there also always exists always one version of Azure.
     * If you want a fixed version, check https://hub.docker.com/_/microsoft-azure-storage-azurite
     */
    String dockerImageVersion = "latest";

    int port = 10000;
}
