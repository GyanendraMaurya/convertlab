package com.convertlab.convertlab_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ConvertlabBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConvertlabBackendApplication.class, args);
	}

}
