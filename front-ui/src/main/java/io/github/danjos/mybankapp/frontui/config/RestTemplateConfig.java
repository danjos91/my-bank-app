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
            
            // Try to get token from session
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpSession session = attributes.getRequest().getSession(false);
                if (session != null) {
                    String accessToken = (String) session.getAttribute("oauth2_access_token");
                    if (accessToken != null) {
                        request.getHeaders().setBearerAuth(accessToken);
                        log.info("OAuth2 token added to request: {}. Session ID: {}", request.getURI(), session.getId());
                    } else {
                        // Log all session attributes for debugging
                        java.util.Enumeration<String> attrNames = session.getAttributeNames();
                        java.util.ArrayList<String> attrs = new java.util.ArrayList<>();
                        while (attrNames.hasMoreElements()) {
                            attrs.add(attrNames.nextElement());
                        }
                        log.error("OAuth2TokenInterceptor: No token found in session for request to: {}. Session ID: {}. Session attributes: {}", 
                            request.getURI(), session.getId(), attrs);
                    }
                } else {
                    log.error("OAuth2TokenInterceptor: No session found for request to: {}", request.getURI());
                }
            } else {
                log.error("OAuth2TokenInterceptor: RequestContextHolder has no attributes for request to: {}. " +
                    "This may happen if RestTemplate is called from a different thread.", request.getURI());
            }
            
            return execution.execute(request, body);
        }
    }
}

