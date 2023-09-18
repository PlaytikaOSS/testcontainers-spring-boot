package com.playtika.testcontainer.opensearch.springdata;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestDocumentRepository extends CrudRepository<TestDocument, String> {
    List<TestDocument> findByTitle(String title);
}
