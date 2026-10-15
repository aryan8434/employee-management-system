package com.example.employees.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI employeeApi() {
        return new OpenAPI().info(new Info()
                .title("Employee Management API")
                .description("Add, find, update and delete employee records.")
                .version("1.0.0"));
    }
}
