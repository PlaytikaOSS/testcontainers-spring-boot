package com.playtika.test.neo4j;

import java.util.List;

import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.repository.CrudRepository;

public interface PersonRepository extends GraphRepository<Person> {

    Person findByName(String name);
}