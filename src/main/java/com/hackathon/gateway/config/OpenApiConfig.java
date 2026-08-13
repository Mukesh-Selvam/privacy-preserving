package com.hackathon.gateway.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 documentation configuration.
 *
 * <p>Documents the API Key security scheme so Swagger UI provides an
 * "Authorize" button for testers and partner developers.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String apiKeyScheme = "X-API-Key";

        return new OpenAPI()
                .info(new Info()
                        .title("Privacy-Preserving Data Sharing Gateway API")
                        .description("""
                                **PS26SCS211** — Enterprise API for consent-aware, policy-gated patient data access.
                                
                                Every field access is evaluated against two independent gates:
                                1. **Organisation-level OPA policy** — what the org is allowed to see.
                                2. **Patient consent** — what the patient has approved for sharing.
                                
                                Only when both agree is a field returned — encrypted for reversible fields, masked otherwise.
                                All transactions are immutably logged to the audit trail.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Privacy Gateway Team")
                                .email("gateway@hackathon.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList(apiKeyScheme))
                .components(new Components()
                        .addSecuritySchemes(apiKeyScheme, new SecurityScheme()
                                .name(apiKeyScheme)
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .description("API key required for all partner and client requests. Use `gateway-demo-key` for development.")));
    }
}
