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

    static final String DEFAULT_CERT_CLASSPATH = "azurite/cert.pem";
    static final String DEFAULT_KEY_CLASSPATH = "azurite/key.pem";

    int blobStoragePort = 10000;
    int queueStoragePort = 10001;
    int tableStoragePort = 10002;

    /**
     * Enables HTTPS for Azurite. Required for OAuth (DefaultAzureCredential) support.
     * When enabled without providing cert/key paths, uses an embedded self-signed certificate.
     */
    boolean httpsEnabled = false;

    /**
     * Enables OAuth basic emulation (--oauth basic). Requires httpsEnabled=true.
     * Allows using DefaultAzureCredential with Azurite.
     */
    boolean oauthEnabled = false;

    /**
     * Classpath or file path to a PEM certificate for HTTPS. Used together with pemKeyPath.
     * If not set when httpsEnabled=true, the embedded self-signed certificate is used.
     */
    String pemCertPath;

    /**
     * Classpath or file path to a PEM private key for HTTPS. Used together with pemCertPath.
     */
    String pemKeyPath;

    /**
     * Classpath or file path to a PFX certificate for HTTPS. Used together with pfxPassword.
     */
    String pfxCertPath;

    /**
     * Password for the PFX certificate.
     */
    String pfxPassword;

    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "mcr.microsoft.com/azure-storage/azurite:3.35.0";
    }
}
