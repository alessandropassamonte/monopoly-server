package com.monopoly.server.monopoly.configurations;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsGlobalConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")      // tutte le URL
                .allowedOriginPatterns("*")   // qualunque origine
                .allowedMethods("*")    // GET, POST, PUT, DELETE, ecc.
                .allowedHeaders("*")    // tutti gli header
                .allowCredentials(true) // se ti servono i cookie / Authorization
                .maxAge(3600);          // caching pre‑flight per 1 h
    }
}

