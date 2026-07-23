package com.medsupply.platform.config;

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
 * Swagger/OpenAPI Configuration class.
 * Automatically configures Bearer Token inputs on all protected controller pathways.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI medSupplyOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("MedSupply Enterprise Platform API")
                        .description("Production-grade secure supply chain platform backend specifications.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("MedSupply Compliance Division")
                                .email("compliance@medsupply.com")
                                .url("https://medsupply.com"))
                        .license(new License()
                                .name("Proprietary License")
                                .url("https://medsupply.com/license")))
                .addSecurityItem(new SecurityRequirement()
                        .addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Inbound Bearer JWT authorization token. Pass: 'Bearer {token}'")));
    }
}
