package com.bank.banking_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableMethodSecurity
@EnableAspectJAutoProxy
public class BankingApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankingApiApplication.class, args);
	}

}
