package com.hyltest.rag_practice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfiguration implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                // 暴露 Content-Disposition 头，前端可以读取文件名
                // 暴露 Content-Type 头，前端可以正确识别文件类型
                .exposedHeaders("Content-Disposition", "Content-Type");
    }
}