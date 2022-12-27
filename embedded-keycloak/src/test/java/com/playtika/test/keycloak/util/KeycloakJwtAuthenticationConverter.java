package com.playtika.test.keycloak.util;

import lombok.AllArgsConstructor;
import lombok.val;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.testcontainers.shaded.com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;

@AllArgsConstructor
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final String clientId;

    @Override
    public AbstractAuthenticationToken convert(Jwt source) {
        val authorities = new HashSet<>(new JwtGrantedAuthoritiesConverter().convert(source));
        val roles = extractResourceRoles(source);
        return new JwtAuthenticationToken(source, Sets.union(authorities, roles));
    }

    private Set<? extends GrantedAuthority> extractResourceRoles(Jwt jwt) {
        var resourceAccess = new HashMap<String, Map<String, List<String>>>(jwt.getClaim("resource_access"));
        var resourceRoles = new ArrayList<String>();

        if (resourceAccess.containsKey(clientId)) {
            var resource = (Map<String, List<String>>) resourceAccess.get(clientId);
            resourceRoles.addAll(resource.get("roles"));
        }

        if (resourceRoles.isEmpty()) {
            return emptySet();
        } else {
            return resourceRoles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).collect(toSet());
        }
    }
}
