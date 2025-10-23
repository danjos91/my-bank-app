package io.github.danjos.mybankapp.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
public class LoggingGatewayFilterFactory extends AbstractGatewayFilterFactory<LoggingGatewayFilterFactory.Config> {

    public LoggingGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();
            
            String requestId = java.util.UUID.randomUUID().toString();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            // Log incoming request
            log.info("Gateway Request [{}] - {} {} from {} at {}", 
                    requestId,
                    request.getMethod(),
                    request.getURI(),
                    request.getRemoteAddress(),
                    timestamp);
            
            // Add request ID to headers
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-Request-ID", requestId)
                    .header("X-Gateway-Timestamp", timestamp)
                    .build();
            
            return chain.filter(exchange.mutate().request(modifiedRequest).build())
                    .then(Mono.fromRunnable(() -> {
                        // Log response
                        log.info("Gateway Response [{}] - Status: {} at {}", 
                                requestId,
                                response.getStatusCode(),
                                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    }));
        };
    }

    public static class Config {
        private boolean logHeaders = false;
        private boolean logBody = false;
        
        public boolean isLogHeaders() {
            return logHeaders;
        }
        
        public void setLogHeaders(boolean logHeaders) {
            this.logHeaders = logHeaders;
        }
        
        public boolean isLogBody() {
            return logBody;
        }
        
        public void setLogBody(boolean logBody) {
            this.logBody = logBody;
        }
    }
}
