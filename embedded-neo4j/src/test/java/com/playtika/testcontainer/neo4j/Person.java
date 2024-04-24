package com.playtika.testcontainer.neo4j;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;

@Node
@NoArgsConstructor
@Getter
@Setter
public class Person {

    @Id
    @GeneratedValue
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
    @Relationship(type = "TEAMMATE")
    public Set<TeamMateRelationship> teammates;

    public void worksWith(TeamMateRelationship person) {
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
                .map(teamMateRelationship -> teamMateRelationship.getTeamMate().getName())
                .collect(toList());
    }
}
