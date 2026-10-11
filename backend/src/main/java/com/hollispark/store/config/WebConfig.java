package com.hollispark.store.config;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final StoreProperties props;

    public WebConfig(StoreProperties props) {
        this.props = props;
    }

    /** Only needed when a frontend is served from another origin; behind CloudFront /api is same-origin. */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = props.corsAllowedOrigins() == null ? List.of()
                : props.corsAllowedOrigins().stream().map(String::trim).filter(o -> !o.isBlank()).toList();
        if (origins.isEmpty()) return;
        registry.addMapping("/api/**")
                .allowedOrigins(origins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowCredentials(true)
                .allowedHeaders("Content-Type", "X-XSRF-TOKEN")
                .maxAge(3600);
    }
}
