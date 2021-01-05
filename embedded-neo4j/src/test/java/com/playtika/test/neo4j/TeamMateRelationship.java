package com.playtika.test.neo4j;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
@RequiredArgsConstructor
@Getter
@Setter
public class TeamMateRelationship {

    private final Integer since;

    @TargetNode
    private Person teamMate;

}
