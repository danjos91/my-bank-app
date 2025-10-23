package io.github.danjos.mybankapp.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class RateLimitingGatewayFilterFactory extends AbstractGatewayFilterFactory<RateLimitingGatewayFilterFactory.Config> {

    private final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastResetTime = new ConcurrentHashMap<>();

    public RateLimitingGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String clientId = getClientId(request);
            
            if (isRateLimited(clientId, config)) {
                log.warn("Rate limit exceeded for client: {}", clientId);
                return handleRateLimitExceeded(exchange, config);
            }
            
            return chain.filter(exchange);
        };
    }

    private String getClientId(ServerHttpRequest request) {
        String clientIp = request.getRemoteAddress() != null ? 
                request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
        String userAgent = request.getHeaders().getFirst("User-Agent");
        return clientIp + ":" + (userAgent != null ? userAgent.hashCode() : "unknown");
    }

    private boolean isRateLimited(String clientId, Config config) {
        long currentTime = System.currentTimeMillis();
        long windowStart = currentTime - (config.getWindowSizeInSeconds() * 1000L);
        
        // Reset counter if window has passed
        if (lastResetTime.getOrDefault(clientId, 0L) < windowStart) {
            requestCounts.put(clientId, new AtomicInteger(0));
            lastResetTime.put(clientId, currentTime);
        }
        
        AtomicInteger count = requestCounts.computeIfAbsent(clientId, k -> new AtomicInteger(0));
        return count.incrementAndGet() > config.getMaxRequests();
    }

    private Mono<Void> handleRateLimitExceeded(org.springframework.web.server.ServerWebExchange exchange, Config config) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add("X-Rate-Limit-Limit", String.valueOf(config.getMaxRequests()));
        response.getHeaders().add("X-Rate-Limit-Remaining", "0");
        response.getHeaders().add("X-Rate-Limit-Reset", String.valueOf(System.currentTimeMillis() + (config.getWindowSizeInSeconds() * 1000L)));
        
        String body = String.format("{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Limit: %d per %d seconds\",\"timestamp\":\"%s\"}", 
                config.getMaxRequests(), 
                config.getWindowSizeInSeconds(),
                LocalDateTime.now().toString());
        
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {
        private int maxRequests = 100;
        private int windowSizeInSeconds = 60;
        
        public int getMaxRequests() {
            return maxRequests;
        }
        
        public void setMaxRequests(int maxRequests) {
            this.maxRequests = maxRequests;
        }
        
        public int getWindowSizeInSeconds() {
            return windowSizeInSeconds;
        }
        
        public void setWindowSizeInSeconds(int windowSizeInSeconds) {
            this.windowSizeInSeconds = windowSizeInSeconds;
        }
    }
}
