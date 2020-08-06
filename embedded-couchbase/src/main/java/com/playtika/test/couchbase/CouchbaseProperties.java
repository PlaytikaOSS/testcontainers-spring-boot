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
package com.playtika.test.couchbase;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.testcontainers.couchbase.CouchbaseService;

import static java.lang.String.format;

/**
 * https://blog.couchbase.com/testing-spring-data-couchbase-applications-with-testcontainers/
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.couchbase")
public class CouchbaseProperties extends CommonContainerProperties {
    public static final String BEAN_NAME_EMBEDDED_COUCHBASE = "embeddedCouchbase";
    CouchbaseService[] services = new CouchbaseService[]{
            CouchbaseService.INDEX
            , CouchbaseService.KV
            , CouchbaseService.QUERY
            , CouchbaseService.SEARCH};
    String dockerImage = "couchbase/server:6.5.1";
    int bucketRamMb = 100;
    String bucketType = "couchbase";

    String host = "localhost";
    String user = "Administrator";
    String password = "password";
    String bucket = "test";

    public void setPassword(String password) {
        if (password.length() < 6) {
            throw new IllegalArgumentException("Couchbase requires password length >= 6 chars, password=" + password);
        }
        this.password = password;
    }

    public String getCredentials() {
        return format("%s:%s", user, password);
    }
}

