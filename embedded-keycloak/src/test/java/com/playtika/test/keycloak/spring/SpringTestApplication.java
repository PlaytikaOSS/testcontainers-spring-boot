package com.playtika.test.keycloak.spring;

import static org.keycloak.adapters.KeycloakDeploymentBuilder.build;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import javax.annotation.security.RolesAllowed;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.springsecurity.KeycloakConfiguration;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@EnableWebMvc
@KeycloakConfiguration
@EnableGlobalMethodSecurity(jsr250Enabled = true)
@SpringBootApplication
public class SpringTestApplication extends KeycloakWebSecurityConfigurerAdapter {

    @Value("${embedded.keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${testing.keycloak.realm}")
    private String realm;

    @Value("${testing.keycloak.client}")
    private String client;

    @Value("${testing.keycloak.client-secret}")
    private String clientSecret;

    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    protected AdapterDeploymentContext adapterDeploymentContext() {
        AdapterConfig config = new AdapterConfig();
        config.setAuthServerUrl(authServerUrl);
        config.setRealm(realm);
        config.setResource(client);
        config.setClientKeyPassword(clientSecret);
        config.setBearerOnly(true);
        config.setUseResourceRoleMappings(true);

        return new AdapterDeploymentContext(build(config));
    }

    @Autowired
    public void configureGlobal(final AuthenticationManagerBuilder auth) {
        KeycloakAuthenticationProvider authenticationProvider = keycloakAuthenticationProvider();
        authenticationProvider.setGrantedAuthoritiesMapper(new SimpleAuthorityMapper());
        auth.authenticationProvider(authenticationProvider);
    }

    @Override
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new NullAuthenticatedSessionStrategy();
    }

    @Override
    protected void configure(final HttpSecurity http) throws Exception {
        super.configure(http);

        //@formatter:off
        http.cors().and()
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(STATELESS).and()
            .formLogin().disable()
            .httpBasic().disable()
            .authorizeRequests()
            .antMatchers("/api/**").fullyAuthenticated()
            .anyRequest().denyAll();
        //@formatter:on
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
