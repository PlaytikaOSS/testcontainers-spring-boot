/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Playtika
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
package com.playtika.test.kafka.properties;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.playtika.test.kafka.properties.SchemaRegistryConfigurationProperties.Authentication.BASIC;
import static com.playtika.test.kafka.properties.SchemaRegistryConfigurationProperties.Authentication.NONE;
import static com.playtika.test.kafka.properties.SchemaRegistryConfigurationProperties.AvroCompatibilityLevel.BACKWARD;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.kafka.schema-registry")
public class SchemaRegistryConfigurationProperties extends CommonContainerProperties {

    public static final String SCHEMA_REGISTRY_BEAN_NAME = "schema-registry";
    public static final String USERNAME = "admin";
    public static final String PASSWORD = "letmein";

    private String dockerImage = "confluentinc/cp-schema-registry:5.5.1";
    private int port = 8081;
    private AvroCompatibilityLevel avroCompatibilityLevel = BACKWARD;
    private Authentication authentication = NONE;

    public boolean isBasicAuthenticationEnabled() {
        return authentication == BASIC;
    }

    public enum AvroCompatibilityLevel {

        NONE, BACKWARD, BACKWARD_TRANSITIVE, FORWARD, FORWARD_TRANSITIVE, FULL, FULL_TRANSITIVE
    }

    public enum Authentication {

        NONE, BASIC
    }
}
