package com.hollispark.store.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API description at /api/v1/docs and Swagger UI at /api/v1/docs/ui (local and dev only).
 * Every endpoint answers with the ApiResponse envelope; admin endpoints need a staff session cookie
 * and, for writes, the X-XSRF-TOKEN header from GET /api/v1/auth/csrf.
 */
@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI storeOpenApi(StoreProperties props) {
        return new OpenAPI().info(new Info()
                .title(props.name() + " store API")
                .version("v1")
                .description("Storefront, checkout, payments and the admin panel. Responses use the envelope "
                        + "{success, status, message, data[, requestId]}."));
    }
}
