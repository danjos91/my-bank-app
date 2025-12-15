package io.github.danjos.mybankapp.authserver.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Metrics collector for authentication operations.
 * Tracks successful and failed login attempts with detailed reasons.
 */
@Component
public class AuthMetrics {

    private final Counter successfulLoginsCounter;
    private final Counter failedLoginsInvalidCredentials;
    private final Counter failedLoginsUnsupportedGrantType;
    private final Counter failedLoginsUnknownError;

    public AuthMetrics(MeterRegistry meterRegistry) {
        this.successfulLoginsCounter = Counter.builder("auth.login.success")
                .description("Number of successful login attempts")
                .tag("type", "authentication")
                .register(meterRegistry);

        this.failedLoginsInvalidCredentials = Counter.builder("auth.login.failed")
                .description("Number of failed login attempts due to invalid credentials")
                .tag("reason", "invalid_credentials")
                .register(meterRegistry);

        this.failedLoginsUnsupportedGrantType = Counter.builder("auth.login.failed")
                .description("Number of failed login attempts due to unsupported grant type")
                .tag("reason", "unsupported_grant_type")
                .register(meterRegistry);

        this.failedLoginsUnknownError = Counter.builder("auth.login.failed")
                .description("Number of failed login attempts due to unknown errors")
                .tag("reason", "unknown_error")
                .register(meterRegistry);
    }

    /**
     * Record a successful login attempt.
     */
    public void recordSuccessfulLogin() {
        successfulLoginsCounter.increment();
    }

    /**
     * Record a failed login attempt with invalid credentials.
     */
    public void recordFailedLoginInvalidCredentials() {
        failedLoginsInvalidCredentials.increment();
    }

    /**
     * Record a failed login attempt with unsupported grant type.
     */
    public void recordFailedLoginUnsupportedGrantType() {
        failedLoginsUnsupportedGrantType.increment();
    }

    /**
     * Record a failed login attempt with unknown error.
     */
    public void recordFailedLoginUnknownError() {
        failedLoginsUnknownError.increment();
    }
}

