package com.playtika.testcontainer.dynamodb;

import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.dynamodb")
public class DynamoDBProperties extends CommonContainerProperties {

    static final String BEAN_NAME_EMBEDDED_DYNAMODB = "embeddedDynamoDb";

    String host = "localhost";
    int port = 8000;

    String accessKey = "n/a";
    String secretKey = "n/a";

    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "amazon/dynamodb-local:2.2.0";
    }
}
