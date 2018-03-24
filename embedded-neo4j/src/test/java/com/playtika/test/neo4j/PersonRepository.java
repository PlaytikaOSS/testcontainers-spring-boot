package com.playtika.test.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface PersonRepository extends Neo4jRepository<Person, String> {

    Person findByName(String name);
}