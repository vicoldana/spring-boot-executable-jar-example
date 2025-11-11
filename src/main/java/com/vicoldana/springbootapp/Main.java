package com.vicoldana.springbootapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class Main {

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @GetMapping("/")
    public String home() {
        return "✅ Hello from Spring Boot Web App running on port 8080!";
    }

    @GetMapping("/health")
    public String health() {
        return "🟢 Application is healthy!";
    }
}
