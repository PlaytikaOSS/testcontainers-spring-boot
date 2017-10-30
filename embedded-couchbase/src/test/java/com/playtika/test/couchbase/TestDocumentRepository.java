package com.playtika.test.couchbase;

import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.stereotype.Repository;

@Repository
interface TestDocumentRepository extends CouchbaseRepository<TestDocument, String> {
}
