package com.playtika.testcontainer.couchbase.springdata;

import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestDocumentRepository extends CouchbaseRepository<TestDocument, String> {

    List<TestDocument> findByTitle(String title);
}
