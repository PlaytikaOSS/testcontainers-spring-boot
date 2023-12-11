package com.playtika.testcontainer.minio;

import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.minio")
public class MinioProperties extends CommonContainerProperties {

    static final String BEAN_NAME_EMBEDDED_MINIO = "minio";
    static final String BEAN_NAME_EMBEDDED_MINIO_PACKAGE_PROPERTIES = "minioPackageProperties";

    String accessKey = "AKIAIOSFODNN7EXAMPLE";
    String secretKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";
    String region = "";
    String worm = "off";
    String browser = "on";
    String directory = "/data";

    String host = "localhost";
    int port = 9000;
    int consolePort = 9001;

    // https://hub.docker.com/r/minio/minio
    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "minio/minio:RELEASE.2023-03-13T19-46-17Z";
    }
}
