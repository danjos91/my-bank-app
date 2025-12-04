package io.github.danjos.mybankapp.authserver.controller;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class TokenController {

    private static final Logger log = Logger.getLogger(TokenController.class.getName());
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final JWKSource<SecurityContext> jwkSource;
    private final String issuer;

    public TokenController(AuthenticationManager authenticationManager,
                           JwtEncoder jwtEncoder,
                           JWKSource<SecurityContext> jwkSource,
                           @Value("${spring.security.oauth2.authorization-server.issuer:http://my-bank-app-auth-server:8085}") String issuer) {
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.jwkSource = jwkSource;
        this.issuer = issuer;
    }

    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Map<String, Object>> getToken(
            @RequestParam("grant_type") String grantType,
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam(value = "scope", required = false) String scope) {
        
        if (!"password".equals(grantType)) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "unsupported_grant_type");
            error.put("error_description", "Only 'password' grant type is supported");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            
            // Build JWT claims
            Instant now = Instant.now();
            String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));

            JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(userDetails.getUsername())
                .audience(java.util.Collections.singletonList("bank-app"))
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS))
                .claim("scope", StringUtils.hasText(scope) ? scope : "read write")
                .claim("authorities", authorities)
                .build();

            // Ensure we use the same key ID as the JWK Source
            String keyId = "bank-app-key-id"; // Fallback
            try {
                List<JWK> keys = jwkSource.get(new JWKSelector(new JWKMatcher.Builder().build()), null);
                if (!keys.isEmpty()) {
                    keyId = keys.get(0).getKeyID();
                    log.info("Using Key ID from JWKSource: " + keyId);
                }
            } catch (Exception e) {
                log.warning("Could not fetch key ID from JWKSource: " + e.getMessage());
            }

            JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(keyId)
                .build();
            String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

            Map<String, Object> response = new HashMap<>();
            response.put("access_token", token);
            response.put("token_type", "Bearer");
            response.put("expires_in", 3600);
            response.put("scope", StringUtils.hasText(scope) ? scope : "read write");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.severe("Error generating token for user: " + username + " - " + e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "invalid_grant");
            error.put("error_description", "Invalid username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }
}

