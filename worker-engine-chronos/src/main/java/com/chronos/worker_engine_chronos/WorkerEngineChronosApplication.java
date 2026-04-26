package com.chronos.worker_engine_chronos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableRabbit
@EnableScheduling
public class WorkerEngineChronosApplication {

	public static void main(String[] args) {
		SpringApplication.run(WorkerEngineChronosApplication.class, args);
	}
}
