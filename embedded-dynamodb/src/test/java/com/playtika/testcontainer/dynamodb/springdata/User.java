package com.playtika.testcontainer.dynamodb.springdata;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Data
@DynamoDbBean
public class User {

    private String id;
    private String firstName;

    public User() {
    }

    public User(String id, String firstName) {
        this.id = id;
        this.firstName = firstName;
    }

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    @DynamoDbAttribute("firstName")
    public String getFirstName() {
        return firstName;
    }
}
