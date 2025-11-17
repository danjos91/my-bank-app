package io.github.danjos.mybankapp.frontui.config;

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
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(Collections.singletonList(new OAuth2TokenInterceptor()));
        return restTemplate;
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
                    }
                }
            }
            
            return execution.execute(request, body);
        }
    }
}

