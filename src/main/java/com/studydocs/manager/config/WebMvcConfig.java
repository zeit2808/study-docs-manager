package com.studydocs.manager.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:uploads/avatars}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve avatar files từ thư mục uploads/avatars
        String uploadPath = Paths.get(uploadDir).toAbsolutePath().toString();
        registry.addResourceHandler("/static/avatars/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }
}

