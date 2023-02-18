package com.playtika.test.db2;

import com.playtika.test.common.properties.CommonContainerProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.db2")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Db2Properties extends CommonContainerProperties {
    public static final String BEAN_NAME_EMBEDDED_DB2 = "embeddedDb2";

    @NotBlank
    String user = "db2inst1";
    @NotBlank
    String password = "foobar1234";
    @NotBlank
    String database = "test";

    boolean acceptLicence = false;

    String startupLogCheckRegex;
    String initScriptPath;

    @Override
    public String getDefaultDockerImage() {
        return "ibmcom/db2";
    }
}
