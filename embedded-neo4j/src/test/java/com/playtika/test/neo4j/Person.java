package com.playtika.test.neo4j;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;

@NodeEntity
@NoArgsConstructor
@Getter
@Setter
public class Person {

    @GraphId
    private Long id;

    private String name;

    public Person(String name) {
        this.name = name;
    }

    /**
     * Neo4j doesn't REALLY have bi-directional relationships. It just means when querying
     * to ignore the direction of the relationship.
     * https://dzone.com/articles/modelling-data-neo4j
     */
    @Relationship(type = "TEAMMATE", direction = Relationship.UNDIRECTED)
    public Set<Person> teammates;

    public void worksWith(Person person) {
        if (teammates == null) {
            teammates = new HashSet<>();
        }
        teammates.add(person);
    }

    public String toString() {
        return this.name + "'s teammates => "
                + Optional.ofNullable(this.teammates)
                .orElse(emptySet())
                .stream()
                .map(Person::getName)
                .collect(toList());
    }
}
