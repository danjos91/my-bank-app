package io.github.danjos.mybankapp.transfer.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.Duration;

@Configuration
@Slf4j
public class TransferServiceConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // Use RestTemplateBuilder to preserve auto-configured interceptors (including tracing)
        return builder
                .additionalInterceptors(new JwtTokenInterceptor())
                .build();
    }

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(10))
                        .build())
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(30))
                        .slidingWindowSize(10)
                        .minimumNumberOfCalls(5)
                        .build())
                .build());
    }

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> accountsServiceCustomizer() {
        return factory -> factory.configure(builder -> builder
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(15))
                        .build())
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .failureRateThreshold(60)
                        .waitDurationInOpenState(Duration.ofSeconds(45))
                        .slidingWindowSize(15)
                        .minimumNumberOfCalls(3)
                        .build())
                .build(), "accounts-service");
    }

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> notificationsServiceCustomizer() {
        return factory -> factory.configure(builder -> builder
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(5))
                        .build())
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .failureRateThreshold(70)
                        .waitDurationInOpenState(Duration.ofSeconds(20))
                        .slidingWindowSize(20)
                        .minimumNumberOfCalls(5)
                        .build())
                .build(), "notifications-service");
    }

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> transferServiceCustomizer() {
        return factory -> factory.configure(builder -> builder
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(30))
                        .build())
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .failureRateThreshold(40)
                        .waitDurationInOpenState(Duration.ofSeconds(60))
                        .slidingWindowSize(20)
                        .minimumNumberOfCalls(5)
                        .build())
                .build(), "transfer-service");
    }

    private static class JwtTokenInterceptor implements ClientHttpRequestInterceptor {
        
        @Override
        public ClientHttpResponse intercept(
                HttpRequest request,
                byte[] body,
                ClientHttpRequestExecution execution) throws IOException {
            
            // Try to get token from request headers first (most reliable)
            String token = null;
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest httpRequest = attributes.getRequest();
                String authHeader = httpRequest.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            }
            
            // Fallback: Extract from SecurityContext if not in headers
            if (token == null) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
                    Jwt jwt = (Jwt) authentication.getPrincipal();
                    token = jwt.getTokenValue();
                }
            }
            
            if (token != null) {
                request.getHeaders().setBearerAuth(token);
                log.debug("JWT token added to request: {}", request.getURI());
            } else {
                log.warn("No JWT token found for request to: {}", request.getURI());
            }
            
            return execution.execute(request, body);
        }
    }
}
