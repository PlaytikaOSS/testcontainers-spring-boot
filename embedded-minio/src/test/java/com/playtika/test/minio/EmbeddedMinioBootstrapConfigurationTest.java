package com.playtika.test.minio;

import com.playtika.test.common.utils.ThrowingRunnable;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;
import org.testcontainers.containers.ToxiproxyContainer;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = EmbeddedMinioBootstrapConfigurationTest.MinioTestConfiguration.class,
        properties = {
                "embedded.toxiproxy.proxies.minio.enabled=true"
        }
)
public class EmbeddedMinioBootstrapConfigurationTest {

    private static final String BUCKET = "my-test-bucket";

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioClient minioToxiProxyClient;

    @Autowired
    ToxiproxyContainer.ContainerProxy minioContainerProxy;

    @BeforeEach
    public void setUp() throws Exception {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(BUCKET).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
        }
    }

    @Test
    public void shouldSuccessfullyWriteAndReadData() throws Exception {
        writeFileToMinio("example.txt", getFilePath("example.txt"));

        String content = readFileFromMinio("example.txt");

        assertThat(content).isEqualTo("Hello Minio!");
    }

    @Test
    public void latencyIsSlower() throws Exception {
        minioContainerProxy.toxics().latency("latency", ToxicDirection.UPSTREAM, 1000);

        assertThat(durationOf(() ->
                minioToxiProxyClient.uploadObject(UploadObjectArgs.builder()
                        .bucket(BUCKET)
                        .object("example.txt")
                        .filename(getFilePath("example.txt"))
                        .build())))
                .isGreaterThan(1000L);

        minioContainerProxy.toxics().get("latency").remove();
    }

    @Test
    public void noLatencyIsFaster() throws Exception {
        assertThat(durationOf(() -> writeFileToMinio("example.txt", getFilePath("example.txt"))))
                .isLessThan(100L);
    }

    private void writeFileToMinio(String fileName, String path) throws Exception {
        minioClient.uploadObject(UploadObjectArgs.builder().bucket(BUCKET).object(fileName).filename(path).build());
    }

    private String readFileFromMinio(String fileName) throws Exception {
        return convertStreamToString(minioClient.getObject(GetObjectArgs.builder().bucket(BUCKET).object(fileName).build()));
    }

    @SneakyThrows
    private String getFilePath(String name) {
        URL resource = this.getClass().getClassLoader().getResource(name);
        if (resource == null) {
            throw new IllegalArgumentException("File " + name + " not found");
        }
        return Paths.get(resource.toURI()).toString();
    }

    private static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is, StandardCharsets.UTF_8.name()).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private static long durationOf(ThrowingRunnable op) throws Exception {
        long start = System.currentTimeMillis();
        op.run();
        return System.currentTimeMillis() - start;
    }

    @EnableAutoConfiguration
    public static class MinioTestConfiguration {

        @Bean
        public MinioClient minioClient(
                @Value("${embedded.minio.port}") int port,
                @Value("${embedded.minio.accessKey}") String accessKey,
                @Value("${embedded.minio.secretKey}") String secretKey,
                @Value("${embedded.minio.region}") String region) {
            MinioClient.Builder minio = MinioClient.builder()
                    .endpoint("http://localhost", port, false)
                    .credentials(accessKey, secretKey);
            if (StringUtils.hasText(region)) {
                minio.region(region);
            }
            return minio.build();
        }

        @Bean
        public MinioClient minioToxiProxyClient(
                @Value("${embedded.minio.toxiproxy.port}") int port,
                @Value("${embedded.minio.accessKey}") String accessKey,
                @Value("${embedded.minio.secretKey}") String secretKey,
                @Value("${embedded.minio.region}") String region) {
            MinioClient.Builder minio = MinioClient.builder()
                    .endpoint("http://localhost", port, false)
                    .credentials(accessKey, secretKey);
            if (StringUtils.hasText(region)) {
                minio.region(region);
            }
            return minio.build();
        }
    }
}
