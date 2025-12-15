package io.github.danjos.mybankapp.exchangegenerator.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // Use RestTemplateBuilder to preserve auto-configured interceptors (including tracing)
        return builder.build();
    }
}

