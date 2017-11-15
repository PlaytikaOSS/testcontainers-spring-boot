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
package com.playtika.test.couchbase.springdata;

import com.playtika.test.couchbase.EmbeddedCouchbaseAutoConfigurationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SpringDataTest extends EmbeddedCouchbaseAutoConfigurationTest {

    @Autowired
    TestDocumentRepository documentRepository;

    @Test
    public void springDataShouldWork() throws Exception {
        String key = "test::1";
        String value = "myvalue";
        assertThat(documentRepository).isNotNull();
        assertThat(documentRepository.exists(key)).isFalse();

        TestDocument testDocument = saveDocument(key, value);

        assertThat(documentRepository.findOne(key)).isEqualTo(testDocument);
    }

    @Test
    public void n1q1ShouldWork() throws Exception {
        String title = "some query title";
        saveDocument("test::2", "custom value");
        saveDocument("test::3", title);
        saveDocument("test::4", title);

        List<TestDocument> resultList = documentRepository.findByTitle(title);

        assertThat(resultList.size()).isEqualTo(2);
    }

    private TestDocument saveDocument(String key, String value) {
        TestDocument testDocument = TestDocument.builder()
                .key(key)
                .title(value)
                .build();
        documentRepository.save(testDocument);
        return testDocument;
    }
}
