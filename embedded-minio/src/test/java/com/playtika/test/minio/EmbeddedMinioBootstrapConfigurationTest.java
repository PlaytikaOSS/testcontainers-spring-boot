package com.playtika.test.minio;

import io.minio.MinioClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = EmbeddedMinioBootstrapConfigurationTest.MinioTestConfiguration.class
)
public class EmbeddedMinioBootstrapConfigurationTest {

    private static final String BUCKET = "my-test-bucket";

    @Value("${embedded.minio.port}")
    int port;

    @Value("${embedded.minio.accessKey}")
    String accessKey;

    @Value("${embedded.minio.secretKey}")
    String secretKey;

    @Value("${embedded.minio.region}")
    String region;

    @Test
    public void shouldSuccessfullyWriteAndReadData() throws Exception {
        MinioClient minioClient = new MinioClient("http://localhost:" + port, accessKey, secretKey, region);

        if (!minioClient.bucketExists(BUCKET)) {
            minioClient.makeBucket(BUCKET);
        }

        String file = getFilePath("example.txt");
        minioClient.putObject(BUCKET, "example.txt", file, null, null, null, null);

        String content = convertStreamToString(minioClient.getObject(BUCKET, "example.txt"));

        assertThat(content).isEqualTo("Hello Minio!");

    }

    private String getFilePath(String name) {
        URL resource = this.getClass().getClassLoader().getResource(name);
        if (resource == null) {
            throw new IllegalArgumentException("File " + name + " not found");
        }
        return resource.getFile();
    }

    private static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is, StandardCharsets.UTF_8.name()).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    @EnableAutoConfiguration
    public static class MinioTestConfiguration {

    }
}