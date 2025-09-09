// src/main/java/com/example/demoapp/config/WebConfig.java
package com.example.demoapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${upload.location:/home/ubuntu/demo/backend/src/main/resources/static/uploads/}")
    private String uploadLocation;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve files from external directory
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + Paths.get(uploadLocation).toAbsolutePath().toString() + "/");
    }
}
