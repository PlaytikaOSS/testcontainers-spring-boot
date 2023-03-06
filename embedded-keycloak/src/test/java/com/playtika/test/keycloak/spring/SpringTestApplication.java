package com.playtika.test.keycloak.spring;

import com.playtika.test.keycloak.util.KeycloakJwtAuthenticationConverter;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@EnableWebMvc
@EnableGlobalMethodSecurity(jsr250Enabled = true)
@SpringBootApplication
public class SpringTestApplication {

    @Value("${testing.keycloak.client}")
    private String client;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests()
                .requestMatchers("/api/**").fullyAuthenticated()
                .and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and().oauth2ResourceServer(oauth2 -> oauth2.jwt().jwtAuthenticationConverter(new KeycloakJwtAuthenticationConverter(client)))
                .formLogin().disable()
                .httpBasic().disable()
                .build();
    }

    @RolesAllowed("role_reader")
    @RestController
    public static class EchoController {

        @GetMapping(path = "/api/echo")
        public String ping() {
            return "pong";
        }
    }
}
