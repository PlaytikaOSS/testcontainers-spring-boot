package com.playtika.test.minio;

import com.github.dockerjava.api.model.Capability;
import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.minio")
public class MinioProperties extends CommonContainerProperties {

    static final String MINIO_BEAN_NAME = "minio";

    String accessKey = "AKIAIOSFODNN7EXAMPLE";
    String secretKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";
    String userName = "root";
    String groupName = "root";
    String region = "";
    String worm = "off";
    String browser = "on";
    String directory = "/data";

    String host = "localhost";
    int port = 9000;

    public MinioProperties() {
        this.setCapabilities(Arrays.asList(Capability.NET_ADMIN));
    }

    // https://hub.docker.com/r/minio/minio
    @Override
    public String getDefaultDockerImage() {
        return "minio/minio:RELEASE.2021-08-05T22-01-19Z";
    }
}
