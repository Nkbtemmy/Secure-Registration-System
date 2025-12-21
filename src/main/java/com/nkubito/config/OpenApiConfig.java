package com.nkubito.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for API documentation.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI secureRegistrationOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Secure Registration System API")
                        .description("""
                                User registration system with post-quantum cryptographic signatures.
                                
                                ## Features
                                - **User Creation**: Admin creates users and receives registration codes
                                - **User Registration**: Users complete registration with email and code
                                - **Dilithium Signatures**: All responses are digitally signed
                                - **Protocol Buffers**: All API communication uses protobuf serialization
                                
                                ## Important Notes
                                - All request/response bodies use `application/octet-stream` (Protocol Buffers)
                                - Every response is wrapped in a `SignedResponse` containing the body and Dilithium signature
                                - Registration codes are 20 hex characters (16 random + 4 checksum)
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("API Support")
                                .email("support@example.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development Server")
                ));
    }
}
