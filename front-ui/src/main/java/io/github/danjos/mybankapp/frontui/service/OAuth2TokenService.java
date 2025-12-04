package io.github.danjos.mybankapp.frontui.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
public class OAuth2TokenService {

    private final RestTemplate tokenRestTemplate;
    
    @Value("${spring.security.oauth2.client.provider.bank-app.token-uri:http://my-bank-app-auth-server:8085/auth/token}")
    private String tokenUri;

    @Value("${spring.security.oauth2.client.registration.bank-app.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.bank-app.client-secret}")
    private String clientSecret;

    public OAuth2TokenService(@Qualifier("tokenRestTemplate") RestTemplate tokenRestTemplate) {
        this.tokenRestTemplate = tokenRestTemplate;
    }

    public String getAccessToken(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            log.error("Username is null or empty when requesting OAuth2 token");
            return null;
        }
        if (password == null || password.trim().isEmpty()) {
            log.error("Password is null or empty when requesting OAuth2 token for user: {}", username);
            return null;
        }
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(clientId, clientSecret);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "password");
            body.add("username", username);
            body.add("password", password);
            body.add("scope", "read write");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            
            // Ensure we're using the correct endpoint
            if (tokenUri == null || tokenUri.isEmpty()) {
                tokenUri = "http://my-bank-app-auth-server:8085/auth/token";
                log.warn("Token URI was null or empty, using default: {}", tokenUri);
            }
            
            log.info("Requesting OAuth2 token from: {} for user: {}", tokenUri, username);
            ResponseEntity<Map<String, Object>> response = tokenRestTemplate.exchange(
                tokenUri,
                HttpMethod.POST,
                request,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            log.info("OAuth2 token response status: {} for user: {}", response.getStatusCode(), username);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object token = response.getBody().get("access_token");
                if (token != null) {
                    log.info("Successfully obtained OAuth2 token for user: {}", username);
                    return token.toString();
                } else {
                    log.error("OAuth2 token response body does not contain access_token for user: {}. Response body: {}", 
                        username, response.getBody());
                }
            } else {
                log.error("OAuth2 token request failed with status: {} for user: {}. Response body: {}", 
                    response.getStatusCode(), username, response.getBody());
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("HTTP error obtaining OAuth2 token for user: {}. Status: {}, Response: {}", 
                username, e.getStatusCode(), e.getResponseBodyAsString(), e);
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Network error connecting to OAuth2 token endpoint for user: {}. Token URI: {}. Error: {}", 
                username, tokenUri, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error obtaining OAuth2 token for user: {}", username, e);
        }
        return null;
    }
}

