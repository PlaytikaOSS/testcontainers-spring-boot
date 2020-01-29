package com.playtika.test.keycloak;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.testcontainers.shaded.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.testcontainers.shaded.com.fasterxml.jackson.annotation.JsonProperty;

@ToString
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
class RealmInfo {

    @JsonProperty
    private String realm;
}
