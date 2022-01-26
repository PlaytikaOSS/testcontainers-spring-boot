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

    String dockerImageVersion = "3.15.0";

    int port = 10000;
}
