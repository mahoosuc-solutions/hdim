package com.healthdata.contracts;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Pact contract testing.
 *
 * <p>Environment variables:
 * <ul>
 *   <li>PACT_BROKER_URL: URL of the Pact Broker (default: http://localhost:9292)</li>
 *   <li>PACT_BROKER_USERNAME: Basic auth username</li>
 *   <li>PACT_BROKER_PASSWORD: Basic auth password</li>
 * </ul>
 *
 * <p>Provider versioning uses git commit information when available, falling back to
 * defaults when not present (useful for local development).
 */
@Configuration
public class PactConfig {

    @Value("${pact.broker.url:http://localhost:9292}")
    private String brokerUrl;

    @Value("${pact.broker.username:hdim}")
    private String brokerUsername;

    @Value("${pact.broker.password:hdimcontract}")
    private String brokerPassword;

    @Value("${pact.provider.version:${git.commit.id:unknown}}")
    private String providerVersion;

    @Value("${pact.provider.branch:${git.branch:main}}")
    private String providerBranch;

    /**
     * Gets the Pact Broker URL.
     *
     * @return the broker URL
     */
    public String getBrokerUrl() {
        return brokerUrl;
    }

    /**
     * Gets the Pact Broker username for authentication.
     *
     * @return the broker username
     */
    public String getBrokerUsername() {
        return brokerUsername;
    }

    /**
     * Gets the Pact Broker password for authentication.
     *
     * @return the broker password
     */
    public String getBrokerPassword() {
        return brokerPassword;
    }

    /**
     * Gets the provider version for contract verification.
     * Uses git commit ID when available, defaults to "unknown".
     *
     * @return the provider version
     */
    public String getProviderVersion() {
        return providerVersion;
    }

    /**
     * Gets the provider branch for contract verification.
     * Uses git branch when available, defaults to "main".
     *
     * @return the provider branch
     */
    public String getProviderBranch() {
        return providerBranch;
    }
}
