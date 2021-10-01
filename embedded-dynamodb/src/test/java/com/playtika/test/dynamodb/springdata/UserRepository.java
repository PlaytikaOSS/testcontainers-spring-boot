package com.playtika.test.dynamodb.springdata;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

@EnableScan
public interface UserRepository extends CrudRepository<User, String> {
    List<User> findByLastName(String lastName);
    List<User> findByFirstName(String firstName);
}
