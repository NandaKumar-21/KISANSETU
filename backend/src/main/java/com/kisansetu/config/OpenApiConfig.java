package com.kisansetu.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI kisanSetuOpenAPI() {
        final String schemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("KisanSetu API")
                        .description("""
                                KisanSetu — India's modern farmer-to-market digital platform.

                                Authentication: every protected endpoint requires a Supabase-issued JWT
                                (Authorization: Bearer <token>). Roles (FARMER, MERCHANT, CUSTOMER,
                                LOGISTICS) are enforced on the backend for every endpoint.
                                """)
                        .version("2.0.0")
                        .license(new License().name("Proprietary")))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components().addSecuritySchemes(schemeName,
                        new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}