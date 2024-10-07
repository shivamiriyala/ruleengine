package com.example.ruleengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RuleengineApplication {

	public static void main(String[] args) {
		SpringApplication.run(RuleengineApplication.class, args);
	}

}
