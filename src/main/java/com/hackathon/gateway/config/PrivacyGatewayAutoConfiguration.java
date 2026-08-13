package com.hackathon.gateway.config;

import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration class imported by {@code @EnablePrivacyGateway}.
 *
 * <p>When any external Spring Boot application annotates its main class with
 * {@code @EnablePrivacyGateway}, this configuration is imported and ensures
 * the gateway's context is recognized as the primary source of beans.
 *
 * <p>When running as a standalone application (which is the standard mode),
 * the component scan in {@link com.hackathon.gateway.GatewayApplication} handles
 * all bean registration. This class intentionally avoids a duplicate
 * {@code @ComponentScan} which would cause CGLIB proxy conflicts.
 *
 * <p>In a future library extraction, this class would carry explicit
 * {@code @Bean} definitions for all exported components.
 */
@Configuration
public class PrivacyGatewayAutoConfiguration {
    // No @ComponentScan here — GatewayApplication's implicit scan handles the current app.
    // Extend this class with explicit @Bean definitions when extracting as a Spring Starter.
}
