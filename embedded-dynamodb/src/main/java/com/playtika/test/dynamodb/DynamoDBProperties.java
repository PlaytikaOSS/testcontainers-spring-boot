package com.playtika.test.dynamodb;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.dynamodb")
public class DynamoDBProperties extends CommonContainerProperties {

    static final String BEAN_NAME_EMBEDDED_DYNAMODB = "embeddedDynamoDb";

    String dockerImage = "amazon/dynamodb-local:latest";
    String host = "localhost";
    int port = 8000;

    String accessKey = "n/a";
    String secretKey = "n/a";

}
