package com.playtika.test.couchbase.springdata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.core.mapping.Field;

@Builder(toBuilder = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Document
public class TestDocument {

    @Id
    String key;

    @Field
    String title;

}
