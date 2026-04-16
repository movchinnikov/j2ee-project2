package com.project.booking.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${server.port:18081}")
    private int port;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Booking Service API")
                        .description("""
                                **Cleaning Service Booking Platform**
                                
                                Manages cleaning orders, properties, cleaner scheduling and earnings.
                                
                                ### Auth
                                Obtain JWT from **IAC Service** (`POST /api/v1/auth/login`) then paste as `Bearer <token>`.
                                """)
                        .version("1.0.0"))
                .servers(List.of(
                        new Server().url("http://localhost:" + port).description("Local"),
                        new Server().url("http://booking-service:8080").description("Docker")
                ))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components().addSecuritySchemes("BearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
