package io.github.danjos.mybankapp.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class OAuth2Config {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://auth-server:8085}")
    private String issuerUri;

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        // Spring Authorization Server exposes JWKS at /oauth2/jwks
        // Try the issuer-uri first, if that doesn't work, use the direct JWKS endpoint
        String jwkSetUri = issuerUri.endsWith("/") 
            ? issuerUri + "oauth2/jwks" 
            : issuerUri + "/oauth2/jwks";
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http, ReactiveJwtDecoder jwtDecoder) {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/auth/**", "/actuator/**", "/health", "/fallback/**").permitAll()
                        .pathMatchers("/api/accounts/users/register", "/api/accounts/login").permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtDecoder(jwtDecoder))
                )
                .build();
    }
}
