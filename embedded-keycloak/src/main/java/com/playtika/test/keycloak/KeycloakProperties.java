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
        "-b 0.0.0.0",
        "-c standalone.xml"
    };

    // https://hub.docker.com/r/jboss/keycloak
    public static final String DEFAULT_KEYCLOAK_IMAGE = "jboss/keycloak:15.0.0";
    public static final String DEFAULT_ADMIN_USER = "admin";
    public static final String DEFAULT_ADMIN_PASSWORD = "letmein";
    public static final String DEFAULT_REALM = "master";
    public static final String DEFAULT_AUTH_BASE_PATH = "/auth";
    public static final String DEFAULT_DB_VENDOR = "h2";

    private String dockerImage = DEFAULT_KEYCLOAK_IMAGE;
    /**
     * The command string issued to the container.
     */
    private String[] command = DEFAULT_COMMAND;
    /**
     * The admin username to use.
     */
    private String adminUser = DEFAULT_ADMIN_USER;
    /**
     * The Keycloak admin password to use.
     */
    private String adminPassword = DEFAULT_ADMIN_PASSWORD;
    /**
     * The relative auth URL of the container. Maybe needs to be tweaked for the WaitStrategy for different Keycloak versions (/auth vs. /auth/).
     */
    private String authBasePath = DEFAULT_AUTH_BASE_PATH;
    /**
     * Classpath location of a JSON file to for importing resources into Keycloak. No prefix is needed.
     */
    private String importFile;
    /**
     * If this is empty then it tries to autodetected. Else it should be one of: h2, postgres, mysql, mariadb, oracle, mssql.
     */
    private String dbVendor = DEFAULT_DB_VENDOR;
    /**
     * Specify hostname of the database (optional).
     */
    private String dbAddr;
    /**
     * Specify port of the database (optional, default is DB vendor default port).
     */
    private String dbPort;
    /**
     * Specify name of the database to use (optional, default is keycloak).
     */
    private String dbDatabase;
    /**
     * Specify name of the schema to use for DB that support schemas (optional, default is public on Postgres).
     */
    private String dbSchema;
    /**
     * Specify user to use to authenticate to the database (optional, default is ``).
     */
    private String dbUser;
    /**
     * Specify user to authenticate to the database via file input (alternative to DB_USER).
     */
    private String dbUserFile;
    /**
     * Specify user's password to use to authenticate to the database (optional, default is ``).
     */
    private String dbPassword;
    /**
     * Specify user's password to use to authenticate to the database via file input (alternative to DB_PASSWORD).
     */
    private String dbPasswordFile;
}
