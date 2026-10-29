package com.codementor.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI codeMentorOpenApi() {
        return new OpenAPI().info(new Info()
                .title("CodeMentor AI")
                .version("1.0.0")
                .description("API de mentor de código com IA: code review, geração de testes e explicações."));
    }
}
