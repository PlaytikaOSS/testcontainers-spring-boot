package com.playtika.testcontainer.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface PersonRepository extends Neo4jRepository<Person, Long> {
    Person findByName(String name);
}