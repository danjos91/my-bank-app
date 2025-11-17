package io.github.danjos.mybankapp.frontui.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
public class OAuth2TokenService {

    private final RestTemplate restTemplate;
    
    @Value("${spring.security.oauth2.client.provider.bank-app.token-uri:http://auth-server:8085/oauth2/token}")
    private String tokenUri;
    
    @Value("${spring.security.oauth2.client.registration.bank-app.client-id:front-ui-client}")
    private String clientId;
    
    @Value("${spring.security.oauth2.client.registration.bank-app.client-secret:front-ui-secret}")
    private String clientSecret;

    public OAuth2TokenService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getAccessToken(String username, String password) {
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
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                tokenUri,
                HttpMethod.POST,
                request,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object token = response.getBody().get("access_token");
                return token != null ? token.toString() : null;
            }
        } catch (Exception e) {
            log.error("Error obtaining OAuth2 token for user: {}", username, e);
        }
        return null;
    }
}

