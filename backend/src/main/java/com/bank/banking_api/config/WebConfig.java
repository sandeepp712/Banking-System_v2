package com.bank.banking_api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")              //Allow all API endpoints
                .allowedOrigins("http://localhost:5173")      // Vite frontend
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Origin","Content-Type","Accept","Authorization","X-Requested-With","Access-Control-Allow-Origin","Access-Control-Allow-Credentials")
                .exposedHeaders("Set-Cookie")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
