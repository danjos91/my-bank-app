package io.github.danjos.mybankapp.frontui.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Collections;

@Configuration
@Slf4j
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(Collections.singletonList(new OAuth2TokenInterceptor()));
        return restTemplate;
    }
    
    @Bean("tokenRestTemplate")
    public RestTemplate tokenRestTemplate() {
        // RestTemplate without interceptor for OAuth2 token requests
        return new RestTemplate();
    }

    private static class OAuth2TokenInterceptor implements ClientHttpRequestInterceptor {
        
        @Override
        public ClientHttpResponse intercept(
                HttpRequest request,
                byte[] body,
                ClientHttpRequestExecution execution) throws IOException {
            
            // Only add token if not already present (BankService methods now add it explicitly)
            if (request.getHeaders().containsKey("Authorization")) {
                log.debug("Authorization header already present in request to: {}", request.getURI());
                return execution.execute(request, body);
            }
            
            // Try to get token from session as fallback
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                try {
                    HttpSession session = attributes.getRequest().getSession(false);
                    if (session != null) {
                        String accessToken = (String) session.getAttribute("oauth2_access_token");
                        if (accessToken != null && !accessToken.trim().isEmpty()) {
                            request.getHeaders().setBearerAuth(accessToken);
                            log.debug("OAuth2 token added to request by interceptor: {}. Session ID: {}", request.getURI(), session.getId());
                        } else {
                            log.warn("OAuth2TokenInterceptor: No token found in session for request to: {}. Session ID: {}. " +
                                "This may cause authentication failures if the request requires authentication.", 
                                request.getURI(), session.getId());
                        }
                    } else {
                        log.debug("OAuth2TokenInterceptor: No session found for request to: {}. " +
                            "This is normal for requests that don't require authentication.", request.getURI());
                    }
                } catch (Exception e) {
                    log.warn("OAuth2TokenInterceptor: Error accessing session for request to: {}. Error: {}", 
                        request.getURI(), e.getMessage());
                }
            } else {
                log.debug("OAuth2TokenInterceptor: RequestContextHolder has no attributes for request to: {}. " +
                    "This may happen if RestTemplate is called from a different thread or async context. " +
                    "BankService methods should handle token passing explicitly.", request.getURI());
            }
            
            return execution.execute(request, body);
        }
    }
}

