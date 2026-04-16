package com.project.iac.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3 / Swagger UI configuration.
 * Adds JWT Bearer auth scheme to all secured endpoints.
 */
@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Value("${server.port:18080}")
    private int serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local development server"),
                        new Server()
                                .url("http://iac-service:" + 8080)
                                .description("Docker internal server")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, jwtSecurityScheme())
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("IAC Service API")
                .description("""
                        **Identity & Access Control Service**
                        
                        Provides authentication, user management, and role-based access control.
                        
                        ### Authentication Flow
                        1. Register via `POST /api/v1/auth/register`
                        2. Login via `POST /api/v1/auth/login` → receive `accessToken`
                        3. Use `Bearer <accessToken>` in the Authorization header
                        4. Refresh via `POST /api/v1/auth/refresh` when token expires
                        
                        ### Default Super Admin
                        - **Username:** `superadmin`
                        - **Password:** `SuperAdmin@123!`
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("J2EE Project Team")
                        .email("team@project.local"))
                .license(new License()
                        .name("Academic Use Only"));
    }

    private SecurityScheme jwtSecurityScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Enter JWT Bearer token. Example: `Bearer eyJhbGci...`");
    }
}
