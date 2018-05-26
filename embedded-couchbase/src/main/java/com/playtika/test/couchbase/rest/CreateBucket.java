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
package com.playtika.test.couchbase.rest;

import com.playtika.test.common.checks.AbstractInitOnStartupStrategy;
import com.playtika.test.couchbase.CouchbaseProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * https://developer.couchbase.com/documentation/server/3.x/admin/REST/rest-bucket-create.html
 */
@Slf4j
@RequiredArgsConstructor
public class CreateBucket extends AbstractInitOnStartupStrategy {

    private final CouchbaseProperties properties;

    @Override
    public String[] getScriptToExecute() {
        return new String[]{
                "curl", "-X", "POST",
                "-u", properties.getCredentials(),
                "http://127.0.0.1:8091/pools/default/buckets",
                "-d", "name=" + properties.getBucket(),
                "-d", "bucketType=" + properties.getBucketType(),
                "-d", "flushEnabled=1",
                "-d", "ramQuotaMB=" + properties.getBucketRamMb(),
                "-d", "replicaNumber=0",
                "-d", "replicaIndex=0",
                "-d", "authType=sasl",
                "-d", "saslPassword=" + properties.getPassword(),
        };
    }

}
