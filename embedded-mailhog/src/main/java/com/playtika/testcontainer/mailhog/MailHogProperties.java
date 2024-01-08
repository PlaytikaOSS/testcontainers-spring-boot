package com.playtika.testcontainer.mailhog;

import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.mailhog")
public class MailHogProperties extends CommonContainerProperties {

    public static final String BEAN_NAME_EMBEDDED_MAILHOG = "embeddedMailHog";

    private Integer smtpPort = 1025;
    private Integer httpPort = 8025;

    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "mailhog/mailhog:v1.0.1";
    }
}
