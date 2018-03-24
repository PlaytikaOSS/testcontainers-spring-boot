/*
* The MIT License (MIT)
*
* Copyright (c) 2017 Playtika
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

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * https://blog.couchbase.com/testing-spring-data-couchbase-applications-with-testcontainers/
 */
@Data
@ConfigurationProperties("embedded.couchbase")
public class CouchbaseProperties {
    public static final String BEAN_NAME_EMBEDDED_COUCHBASE = "embeddedCouchbase";
    boolean enabled;
    String services = "kv,index,n1ql,fts";
    String dockerImage = "couchbase:community-4.5.1";
    int clusterRamMb = 256;
    int bucketRamMb = 100;
    String bucketType = "couchbase";

    String host = "localhost";
    String user = "Administrator";
    String password = "password";
    String bucket = "test";

    int bootstrapHttpDirectPort = 8091;
    int httpDirectPort = 8091;

    int queryServicePort = 8092;
    int queryRestTrafficPort = 8093;
    int searchServicePort = 8094;
    int analyticsServicePort = 8095;

    int memcachedSslPort = 11207;
    int memcachedPort = 11211;

    int bootstrapCarrierDirectPort = 11210;
    int carrierDirectPort = 11210;

    int queryRestTrafficSslPort = 18091;
    int queryServiceSslPort = 18092;
    int n1qlSslPort = 18093;
    int searchServiceHttpsPort = 18094;
}
