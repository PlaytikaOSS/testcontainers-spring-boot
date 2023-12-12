package com.playtika.testcontainer.enterpise.aerospike;

import com.playtika.testcontainer.aerospike.EmbeddedAerospikeBootstrapConfiguration;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.LinkedHashMap;

@Slf4j
@AutoConfiguration(before = EmbeddedAerospikeBootstrapConfiguration.class)
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(value = "embedded.aerospike.enabled", matchIfMissing = true)
public class InjectEnterpriseAerospikeBootstrapConfiguration {

    private static final String DOCKER_IMAGE = "aerospike/aerospike-server-enterprise:6.3.0.16_1";
    private static final String AEROSPIKE_DOCKER_IMAGE_PROPERTY = "embedded.aerospike.dockerImage";

    private ConfigurableEnvironment environment;

    @Autowired
    public void setEnvironment(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void overrideAerospikeImage() {
        log.info("Overriding aerospike Image");

        String dockerImage = environment.getProperty(AEROSPIKE_DOCKER_IMAGE_PROPERTY);
        if (isEnterpriseImage(dockerImage)) {
            return;
        }

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put(AEROSPIKE_DOCKER_IMAGE_PROPERTY, DOCKER_IMAGE);
        MapPropertySource propertySource = new MapPropertySource("modifiedAerospikeProperties", map);
        environment.getPropertySources().addFirst(propertySource);
    }

    private boolean isEnterpriseImage(String dockerImage) {
        //TODO add version check
        return dockerImage != null && dockerImage.contains("enterprise");
    }

}
