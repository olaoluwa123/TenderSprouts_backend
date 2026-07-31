package com.example.backend;

import com.example.backend.config.AppFrontendProperties;
import com.example.backend.config.AppMailProperties;
import com.example.backend.config.AuthProperties;
import com.example.backend.config.JwtProperties;
import com.example.backend.config.SecurityProperties;
import com.example.backend.config.SeedProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
    JwtProperties.class,
    AuthProperties.class,
    SeedProperties.class,
    AppMailProperties.class,
    AppFrontendProperties.class,
    SecurityProperties.class
})
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
