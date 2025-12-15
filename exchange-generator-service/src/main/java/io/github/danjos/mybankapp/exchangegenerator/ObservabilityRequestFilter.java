package io.github.danjos.mybankapp.exchangegenerator;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ObservabilityRequestFilter extends OncePerRequestFilter {

    private static final String SERVICE_NAME = "exchange-generator-service";

    private final Counter requestCounter;
    private final Counter errorCounter;

    public ObservabilityRequestFilter(MeterRegistry meterRegistry) {
        this.requestCounter = Counter.builder("app_http_requests_total")
                .description("Total HTTP requests processed")
                .tag("service", SERVICE_NAME)
                .register(meterRegistry);
        this.errorCounter = Counter.builder("app_http_requests_errors_total")
                .description("HTTP requests resulting in errors")
                .tag("service", SERVICE_NAME)
                .register(meterRegistry);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        requestCounter.increment();
        try {
            filterChain.doFilter(request, response);
            if (response.getStatus() >= 500) {
                errorCounter.increment();
            }
        } catch (Exception ex) {
            errorCounter.increment();
            throw ex;
        }
    }
}
