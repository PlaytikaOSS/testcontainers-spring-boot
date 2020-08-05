/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Playtika
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.playtika.test.keycloak;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.keycloak")
public class KeycloakProperties extends CommonContainerProperties {

    public static final String BEAN_NAME_EMBEDDED_KEYCLOAK = "embeddedKeycloak";

    public static final String[] DEFAULT_COMMAND = {
        "-c",
        "standalone.xml",
        "-Dkeycloak.profile.feature.upload_scripts=enabled"
    };

    public static final String DEFAULT_ADMIN_USER = "admin";
    public static final String DEFAULT_ADMIN_PASSWORD = "letmein";
    public static final String DEFAULT_REALM = "master";

    private String dockerImage = "jboss/keycloak:8.0.1";
    private String[] command = DEFAULT_COMMAND;
    private String adminUser = DEFAULT_ADMIN_USER;
    private String adminPassword = DEFAULT_ADMIN_PASSWORD;
    private String importFile;
    private String dbVendor;
    private String dbAddr;
    private String dbPort;
    private String dbDatabase;
    private String dbSchema;
    private String dbUser;
    private String dbUserFile;
    private String dbPassword;
    private String dbPasswordFile;
}
