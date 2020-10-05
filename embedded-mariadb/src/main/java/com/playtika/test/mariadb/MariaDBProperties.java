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
package com.playtika.test.mariadb;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.mariadb")
public class MariaDBProperties extends CommonContainerProperties {
    static final String BEAN_NAME_EMBEDDED_MARIADB = "embeddedMariaDb";
    String dockerImage = "mariadb:10.3.6";
    String encoding = "utf8mb4";
    String collation = "utf8mb4_unicode_ci";

    String user = "test";
    String password = "test";
    String database = "test";
    String schema = "test_db";
    String host = "localhost";
    int port = 3306;
    long maxAllowedPacket = 16777216;
    String initScriptPath;
}
