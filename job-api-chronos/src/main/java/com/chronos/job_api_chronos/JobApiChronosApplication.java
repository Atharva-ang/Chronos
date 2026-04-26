package com.chronos.job_api_chronos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JobApiChronosApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobApiChronosApplication.class, args);
	}

}
