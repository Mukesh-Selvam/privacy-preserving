package com.hackathon.gateway.annotation;

import com.hackathon.gateway.config.PrivacyGatewayAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Enables Privacy Gateway capabilities for any Spring Boot application / microservice.
 *
 * <p>Importing this annotation registers the AspectJ interceptor, OPA service,
 * Vault key manager, and Redis decision cache configurations seamlessly.
 *
 * <p>Usage:
 * <pre>
 * &#64;SpringBootApplication
 * &#64;EnablePrivacyGateway
 * public class MyMicroserviceApplication {
 *     public static void main(String[] args) {
 *         SpringApplication.run(MyMicroserviceApplication.class, args);
 *     }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(PrivacyGatewayAutoConfiguration.class)
public @interface EnablePrivacyGateway {
}
