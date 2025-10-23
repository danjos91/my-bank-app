package io.github.danjos.mybankapp.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CircuitBreakerConfig {

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
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
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> authServiceCustomizer() {
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
                .build(), "auth-server");
    }

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> accountsServiceCustomizer() {
        return factory -> factory.configure(builder -> builder
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(20))
                        .build())
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .failureRateThreshold(40)
                        .waitDurationInOpenState(Duration.ofSeconds(60))
                        .slidingWindowSize(20)
                        .minimumNumberOfCalls(5)
                        .build())
                .build(), "accounts-service");
    }

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> cashServiceCustomizer() {
        return factory -> factory.configure(builder -> builder
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(25))
                        .build())
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .failureRateThreshold(45)
                        .waitDurationInOpenState(Duration.ofSeconds(45))
                        .slidingWindowSize(15)
                        .minimumNumberOfCalls(3)
                        .build())
                .build(), "cash-service");
    }

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> transferServiceCustomizer() {
        return factory -> factory.configure(builder -> builder
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(30))
                        .build())
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .failureRateThreshold(35)
                        .waitDurationInOpenState(Duration.ofSeconds(60))
                        .slidingWindowSize(20)
                        .minimumNumberOfCalls(5)
                        .build())
                .build(), "transfer-service");
    }

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> notificationsServiceCustomizer() {
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
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> frontUiCustomizer() {
        return factory -> factory.configure(builder -> builder
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(10))
                        .build())
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(30))
                        .slidingWindowSize(10)
                        .minimumNumberOfCalls(3)
                        .build())
                .build(), "front-ui");
    }
}
