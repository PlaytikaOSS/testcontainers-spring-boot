package com.playtika.test.neo4j;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
@RequiredArgsConstructor
@Getter
@Setter
public class TeamMateRelationship {
    @Id
    @GeneratedValue
    private Long id;

    private final Integer since;

    @TargetNode
    private Person teamMate;

}
