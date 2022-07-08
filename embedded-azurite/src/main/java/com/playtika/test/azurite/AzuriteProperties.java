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
     * can't be changed, see https://github.com/Azure/Azurite#default-storage-account
     */
    static final String ACCOUNT_NAME = "devstoreaccount1";

    /**
     * can't be changed, see https://github.com/Azure/Azurite#default-storage-account
     */
    static final String ACCOUNT_KEY = "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";

    int port = 10000;

    @Override
    public String getDefaultDockerImage() {
        return "mcr.microsoft.com/azure-storage/azurite:3.18.0";
    }
}
