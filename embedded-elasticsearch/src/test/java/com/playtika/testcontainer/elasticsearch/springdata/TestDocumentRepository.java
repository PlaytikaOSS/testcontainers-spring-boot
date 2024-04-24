package com.playtika.testcontainer.elasticsearch.springdata;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestDocumentRepository extends ElasticsearchRepository<TestDocument, String> {
    List<TestDocument> findByTitle(String title);
}
