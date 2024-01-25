package com.playtika.testcontainers.aerospike.enterprise;

import com.playtika.testcontainer.aerospike.AerospikeProperties;
import com.playtika.testcontainer.aerospike.EmbeddedAerospikeBootstrapConfiguration;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;

import static com.playtika.testcontainer.aerospike.AerospikeProperties.BEAN_NAME_AEROSPIKE;

@Slf4j
@AutoConfiguration(after = EmbeddedAerospikeBootstrapConfiguration.class)
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(value = "embedded.aerospike.enabled", matchIfMissing = true)
@EnableConfigurationProperties(AerospikeEnterpriseProperties.class)
@PropertySource("classpath:/embedded-enterprise-aerospike.properties")
public class SetupEnterpriseAerospikeBootstrapConfiguration {

    private static final String DOCKER_IMAGE = "aerospike/aerospike-server-enterprise:6.3.0.16";
    private static final String AEROSPIKE_DOCKER_IMAGE_PROPERTY = "embedded.aerospike.dockerImage";
    private static final ImageVersion SUITABLE_IMAGE_VERSION = new ImageVersion(6, 3);
    private static final String TEXT_TO_DOCUMENTATION = "Documentation: https://github.com/PlaytikaOSS/testcontainers-spring-boot/blob/develop/embedded-aerospike-enterprise/README.adoc";

    private GenericContainer<?> aerospikeContainer;
    private AerospikeProperties aerospikeProperties;
    private AerospikeEnterpriseProperties aerospikeEnterpriseProperties;
    private Environment environment;

    @Autowired
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Autowired
    @Qualifier(BEAN_NAME_AEROSPIKE)
    public void setAerospikeContainer(GenericContainer<?> aerospikeContainer) {
        this.aerospikeContainer = aerospikeContainer;
    }

    @Autowired
    public void setAerospikeProperties(AerospikeProperties aerospikeProperties) {
        this.aerospikeProperties = aerospikeProperties;
    }

    @Autowired
    public void setAerospikeEnterpriseProperties(AerospikeEnterpriseProperties aerospikeEnterpriseProperties) {
        this.aerospikeEnterpriseProperties = aerospikeEnterpriseProperties;
    }

    @PostConstruct
    public void setupEnterpriseAerospike() throws IOException, InterruptedException {
        verifyAerospikeImage();
        AerospikeEnterpriseConfigurer aerospikeEnterpriseConfigurer = new AerospikeEnterpriseConfigurer(aerospikeProperties, aerospikeEnterpriseProperties);
        aerospikeEnterpriseConfigurer.configure(aerospikeContainer);
    }

    private void verifyAerospikeImage() {
        log.info("Verify Aerospike Enterprise Image");

        String dockerImage = environment.getProperty(AEROSPIKE_DOCKER_IMAGE_PROPERTY);
        if (dockerImage == null) {
            throw new IllegalStateException("Aerospike enterprise docker image not provided, set up 'embedded.aerospike.dockerImage' property.\n"
                    + TEXT_TO_DOCUMENTATION);
        }

        if (!isEnterpriseImage(dockerImage)) {
            throw illegalAerospikeImageException();
        }
    }

    private IllegalStateException illegalAerospikeImageException() {
        return new IllegalStateException(
                "You should use enterprise image for the Aerospike container with equal or higher version: " + DOCKER_IMAGE + ". "
                        + "Container enable 'disallow-expunge' config option to prevent non-durable deletes, and this config option is available starting with version 6.3. "
                        + "Enterprise image is required, as non-durable deletes are not available in Community."
                        + TEXT_TO_DOCUMENTATION
        );
    }

    private boolean isEnterpriseImage(String dockerImage) {
        return dockerImage.contains("enterprise")
                && isSuitableVersion(dockerImage);
    }

    private boolean isSuitableVersion(String dockerImage) {
        int index = dockerImage.indexOf(":");
        if (index == -1) {
            throw new IllegalStateException("Invalid docker image version format: " + dockerImage + ".\n"
                    + TEXT_TO_DOCUMENTATION);
        }
        String version = dockerImage.substring(index + 1);
        ImageVersion imageVersion = ImageVersion.parse(version);
        return imageVersion.compareTo(SUITABLE_IMAGE_VERSION) >= 0;
    }

}
