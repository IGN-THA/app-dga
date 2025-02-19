package com.docprocess.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;


@Configuration
@EnableWebSecurity
public class SecurityConfig {


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.headers().frameOptions().deny().and().csrf().disable()
                .authorizeRequests(authorizeRequests ->
                        authorizeRequests
                                //.antMatchers("/", "/api/v1/doc/monitor", "/api/v1/doc/syncDocument").permitAll()
                                //.antMatchers("/register").hasRole("ADMIN")
                                .anyRequest().permitAll()
                )
                .headers(headers ->
                        headers
                                .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)
                );

        return http.build();
    }
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("*");  // Allow all origins
        config.addAllowedHeader("*");  // Allow all headers
        config.addAllowedMethod("*");  // Allow all methods
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

}