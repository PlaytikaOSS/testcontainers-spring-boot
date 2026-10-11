package com.playtika.testcontainer.minio;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        classes = EmbeddedMinioBootstrapConfigurationTest.MinioTestConfiguration.class,
        properties = {
                "embedded.toxiproxy.proxies.minio.enabled=true",
                "embedded.containers.parallelStartup=true"
        }
)
class EmbeddedMinioParallelStartupTest extends EmbeddedMinioBootstrapConfigurationTest {
}
