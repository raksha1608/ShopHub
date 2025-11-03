package com.example.user_service;

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity

@SpringBootApplication
@ComponentScan(basePackages = "com.example.user_service")
@EnableWebSecurity
public class UserServiceApplication {

	static void main(String[] args) {
		SpringApplication.run(UserServiceApplication, args);
	}

}
