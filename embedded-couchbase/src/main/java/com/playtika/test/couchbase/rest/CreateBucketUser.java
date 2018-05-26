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
 * Couchbase UI 5.x + charles
 */
@Slf4j
@RequiredArgsConstructor
public class CreateBucketUser extends AbstractInitOnStartupStrategy {

    private final CouchbaseProperties properties;

    @Override
    public String[] getScriptToExecute() {
        return new String[]{
                "curl", "-X", "PUT",
                "-u", properties.getCredentials(),
                "http://127.0.0.1:8091/settings/rbac/users/local/" + properties.getBucket(),
                "-d", "roles=bucket_full_access%5B" + properties.getBucket() + "%5D",
                "-d", "name=",
                "-d", "password=" + properties.getPassword()
        };
    }
}
