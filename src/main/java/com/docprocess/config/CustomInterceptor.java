package com.docprocess.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CustomInterceptor implements WebMvcConfigurer {

    //
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // LogInterceptor apply to all URLs.
        registry.addInterceptor(handleInterceptor()).addPathPatterns("/api/digicert/**").excludePathPatterns("/api/digicert/monitor");

    }

    @Bean
    public DocProcessInterceptor handleInterceptor(){
        return new DocProcessInterceptor();
    }

}
