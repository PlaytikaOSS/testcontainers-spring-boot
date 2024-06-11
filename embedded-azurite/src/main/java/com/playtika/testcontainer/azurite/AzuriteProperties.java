package com.playtika.testcontainer.azurite;

import com.playtika.testcontainer.common.properties.CommonContainerProperties;
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

    int blobStoragePort = 10000;
    int queueStoragePort = 10001;
    int tableStoragePort = 10002;

    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "mcr.microsoft.com/azure-storage/azurite:3.30.0";
    }
}
