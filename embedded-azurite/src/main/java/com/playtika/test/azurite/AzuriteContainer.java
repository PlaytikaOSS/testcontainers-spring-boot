package com.playtika.test.azurite;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class AzuriteContainer extends GenericContainer<AzuriteContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite");

    public AzuriteContainer(String tag) {
        super(DEFAULT_IMAGE_NAME.withTag(tag));
    }

    /**
     * can't be changed, see https://github.com/Azure/Azurite#default-storage-account
     */
    static final String ACCOUNT_NAME = "devstoreaccount1";

    /**
     * can't be changed, see https://github.com/Azure/Azurite#default-storage-account
     */
    static final String ACCOUNT_KEY = "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";
}
