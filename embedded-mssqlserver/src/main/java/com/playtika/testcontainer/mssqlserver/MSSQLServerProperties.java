package com.playtika.testcontainer.mssqlserver;

import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.mssqlserver")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MSSQLServerProperties extends CommonContainerProperties {
    public static final String BEAN_NAME_EMBEDDED_MSSQLSERVER = "embeddedMSSQLServer";

    String password = "Foobar1234!";

    String initScriptPath;
    boolean acceptLicence = false;

    String startupLogCheckRegex;

    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "mcr.microsoft.com/mssql/server:2022-latest";
    }
}
