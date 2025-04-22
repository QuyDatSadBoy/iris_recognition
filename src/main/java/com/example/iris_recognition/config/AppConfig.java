package com.example.iris_recognition.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class AppConfig {

    @Value("${app.upload.max-file-size:10485760}")
    private long maxUploadSize;

    @Value("${app.model.path:src/main/webapp/model/yolov11/}")
    private String modelPath;

    @Bean
    public MultipartResolver multipartResolver() {
        CommonsMultipartResolver resolver = new CommonsMultipartResolver();
        resolver.setMaxUploadSize(maxUploadSize);
        resolver.setDefaultEncoding("UTF-8");
        return resolver;
    }

    @Bean
    public String modelPath() {
        return modelPath;
    }
}
