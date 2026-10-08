package com.wildcard.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String localDir;

    public WebConfig(WildcardProperties props) {
        this.localDir = props.getStorage().getLocalDir();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String path = Paths.get(localDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/files/**", "/uploads/**").addResourceLocations(path);
    }
}
