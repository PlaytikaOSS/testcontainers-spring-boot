package com.playtika.testcontainer.keycloak.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonIgnoreProperties
public final class KeyCloakToken {

    @JsonProperty("access_token")
    private String accessToken;
}
