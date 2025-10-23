package io.github.danjos.mybankapp.gateway.config;

import io.github.danjos.mybankapp.gateway.filter.LoggingGatewayFilterFactory;
import io.github.danjos.mybankapp.gateway.filter.RateLimitingGatewayFilterFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder, 
                                         LoggingGatewayFilterFactory loggingFilter,
                                         RateLimitingGatewayFilterFactory rateLimitingFilter) {
        return builder.routes()
                // Auth Server routes
                .route("auth-server", r -> r
                        .path("/auth/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .filter(loggingFilter.apply(new LoggingGatewayFilterFactory.Config()))
                                .filter(rateLimitingFilter.apply(c -> c.setMaxRequests(50).setWindowSizeInSeconds(60)))
                                .circuitBreaker(config -> config
                                        .setName("auth-server")
                                        .setFallbackUri("forward:/fallback/auth")
                                )
                        )
                        .uri("lb://auth-server")
                )
                
                // Accounts Service routes
                .route("accounts-service", r -> r
                        .path("/api/accounts/**")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingGatewayFilterFactory.Config()))
                                .filter(rateLimitingFilter.apply(c -> c.setMaxRequests(100).setWindowSizeInSeconds(60)))
                                .circuitBreaker(config -> config
                                        .setName("accounts-service")
                                        .setFallbackUri("forward:/fallback/accounts")
                                )
                                .addRequestHeader("X-Gateway-Source", "gateway")
                        )
                        .uri("lb://accounts-service")
                )
                
                // Cash Service routes
                .route("cash-service", r -> r
                        .path("/api/cash/**")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingGatewayFilterFactory.Config()))
                                .filter(rateLimitingFilter.apply(c -> c.setMaxRequests(80).setWindowSizeInSeconds(60)))
                                .circuitBreaker(config -> config
                                        .setName("cash-service")
                                        .setFallbackUri("forward:/fallback/cash")
                                )
                                .addRequestHeader("X-Gateway-Source", "gateway")
                        )
                        .uri("lb://cash-service")
                )
                
                // Transfer Service routes
                .route("transfer-service", r -> r
                        .path("/api/transfers/**")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingGatewayFilterFactory.Config()))
                                .filter(rateLimitingFilter.apply(c -> c.setMaxRequests(60).setWindowSizeInSeconds(60)))
                                .circuitBreaker(config -> config
                                        .setName("transfer-service")
                                        .setFallbackUri("forward:/fallback/transfers")
                                )
                                .addRequestHeader("X-Gateway-Source", "gateway")
                        )
                        .uri("lb://transfer-service")
                )
                
                // Notifications Service routes
                .route("notifications-service", r -> r
                        .path("/api/notifications/**")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingGatewayFilterFactory.Config()))
                                .filter(rateLimitingFilter.apply(c -> c.setMaxRequests(200).setWindowSizeInSeconds(60)))
                                .circuitBreaker(config -> config
                                        .setName("notifications-service")
                                        .setFallbackUri("forward:/fallback/notifications")
                                )
                                .addRequestHeader("X-Gateway-Source", "gateway")
                        )
                        .uri("lb://notifications-service")
                )
                
                // Front UI routes
                .route("front-ui", r -> r
                        .path("/ui/**", "/", "/login", "/register", "/dashboard", "/profile", "/transactions")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingGatewayFilterFactory.Config()))
                                .filter(rateLimitingFilter.apply(c -> c.setMaxRequests(150).setWindowSizeInSeconds(60)))
                                .circuitBreaker(config -> config
                                        .setName("front-ui")
                                        .setFallbackUri("forward:/fallback/ui")
                                )
                                .addRequestHeader("X-Gateway-Source", "gateway")
                        )
                        .uri("lb://front-ui")
                )
                
                // Health check routes
                .route("health-checks", r -> r
                        .path("/actuator/**", "/health")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingGatewayFilterFactory.Config()))
                                .circuitBreaker(config -> config
                                        .setName("health-checks")
                                        .setFallbackUri("forward:/fallback/health")
                                )
                        )
                        .uri("lb://accounts-service")
                )
                
                .build();
    }
}
